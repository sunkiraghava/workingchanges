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

//@Author = Rohit Doraya 
@Listeners(TestListeners.class)
public class EventFrameWorkTestCases {
	static Logger logger = LogManager.getLogger(EventFrameWorkTestCases.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	ObjectMapper mapper = new ObjectMapper();
	private String env, run = "ui";
	private String baseUrl;
	private String segID = "11554400";
	private String segName = "Test MP Audience " + segID;
	private static Map<String, String> dataSet;
	Utilities utils;
	private String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";

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

	//automationtwentytwo fixed
	// verifying guest and rewards events are coming in logs //  creating and deleting new webhook in automationtwentytwo business
	@Test(groups = {"regression", "dailyrun" },priority = 1, description = "SQ-T5818 [EF - Custom Webhook] Custom Webhook Creation with No Authentication Webhook including webhook event sync (Step-1)"
			+ "SQ-T5865 [EF - Custom Webhook] Verification of Marketing Notifications (Step-4.4)")
	public void T5818_ValidateCustomWebhookCreationWithAuthenticationEvents() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookPrefixName = "AutoCustomWebhookNoAuth_";
		String webhookName = CreateDateTime.getUniqueString(webhookPrefixName);
		try {
			List<String> eventNameList = new ArrayList<String>();
			eventNameList.add("Guest");
			eventNameList.add("Loyalty Checkin");
			eventNameList.add("Redemption");
			eventNameList.add("Rewards");
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;
			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");

			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
					"User event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
					"Checkins event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
					"Redemptions event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
					"Rewards event is not appearing in business preferences ");

			logger.info("Verified that all events are coming in business preferences");
			TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

			// create New User
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			String userID = signUpResponse.jsonPath().get("user.user_id").toString();
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			
			// Gift Reedemable to User
			Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", dataSet.get("redeemable_id"));
			Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
					"Status code 201 did not matched for api2 send message to user");
			TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

			// Comment out the below code as it is not working properly -
			// checking logs for Guest event
			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);
			Assert.assertTrue(jsonObjectStr.contains("\"event_name\":\"users\""), "Guest event is not coming in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

			Assert.assertTrue(jsonObjectStr.contains("\"email\":\"" + userEmail + "\""),
					"Loyalty Checkin event is not coming in logs");
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");
						
			// checking logs for rewards

			String jsonObjectStrRewards = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Rewards",
					webhookName,"",timeAfterEventTriggered);

			Assert.assertTrue(jsonObjectStrRewards.contains("\"event_name\":\"rewards\""),
					"Guest event is not coming in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

			Assert.assertTrue(jsonObjectStrRewards.contains("\"email\":\"" + userEmail + "\""),
					"Rewards event is not coming in logs");
			logger.info("Verified that Rewards event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Rewards event is coming in logs");

		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		}
		finally {
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

			}
		}

	}// end of test case T5818_ValidateCustomWebhookCreationWithAuthenticationEvents

	//  creating and deleting new webhook in automationtwentytwo business fixed
	@Test(groups = {"regression", "dailyrun" },priority = 2, description = "SQ-T5819 [EF - Custom Webhook] Custom Webhook Creation with Basic Auth Authentication including webhook event sync (Step-2)")
	public void T5819_ValidateCustomWebhookCreationWithBasicAuthIncludingAuthenticationEvents() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutoTestWebhookBasicAuth");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Guest");
		eventNameList.add("Loyalty Checkin");
		eventNameList.add("Redemption");
		eventNameList.add("Rewards");
		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-2 Base URL (https://flask-webhook-449610.et.r.appspot.com)");

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point",
					"/basic-auth-webhook");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "Basic");
			Thread.sleep(2000);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("User Name", "WebhookAdmin");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Password", "Password");

			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;
			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
					"User event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
					"Checkins event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
					"Redemptions event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
					"Rewards event is not appearing in business preferences ");

			logger.info("Verified that all events are coming in business preferences");
			TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

			// create New User
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
			
			// checking logs for Guest event
			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail,
					"Guest", webhookName, "",timeAfterEventTriggered);
			String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;
			
			Assert.assertEquals(actualEventName,"users" ,  "Guest event is not coming in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");
			
			String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "email") ;

			Assert.assertEquals(actualEmail, userEmail, "Loyalty Checkin event is not coming in logs");
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");
		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

			}
		}

	}// end of test case
		// T5819_ValidateCustomWebhookCreationWithBasicAuthIncludidngAuthenticationEvents

	//automationtwentytwo fixed
	@Test(groups = {"regression", "dailyrun" },priority = 3, description = "SQ-T5820 [EF - Custom Webhook] Custom Webhook Creation with Bearer Authentication including webhook event sync (Step-3)\n")
	public void T5820_ValidateCustomWebhookCreationWithBearerAuthIncludingAuthenticationEvents() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";

		String webhookName = CreateDateTime.getUniqueString("AutoWebhookBearerAuth");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Guest");
		eventNameList.add("Loyalty Checkin");
		eventNameList.add("Redemption");
		eventNameList.add("Rewards");
		boolean isWebhookCreatedSuccessfully = false;
		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
	
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-2 Base URL (https://flask-webhook-449610.et.r.appspot.com)");

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/auth-webhook");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "Bearer");
			Thread.sleep(2000);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Auth Bearer Token",
					"epopbJnj_s5mEzGx3zEd");

			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(true);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;
			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("users"),
					"User event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("checkins"),
					"Checkins event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redemptions"),
					"Redemptions event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("rewards"),
					"Rewards event is not appearing in business preferences ");

			logger.info("Verified that all events are coming in business preferences");
			TestListeners.extentTest.get().info("Verified that all events are coming in business preferences");

			// create New User
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			Thread.sleep(5000);
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

			// checking logs for Guest event
			
			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);

			String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;
			Assert.assertEquals(actualEventName , "users", "Guest event is not coming in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");

			String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "email") ;
			Assert.assertEquals(actualEmail , userEmail, "Loyalty Checkin event is not coming in logs");
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

			}
		}

	}// end of test case
		// T5820_ValidateCustomWebhookCreationWithBearerAuthIncludidngAuthenticationEvents
	//automationtwentytwo
	@Test(groups = {"regression", "dailyrun" },priority = 4, description = "SQ-T5825 [EF - Custom Webhook] Event syncing on Webhook Activation, Deactivation and Deletion (Step 8)")
	public void T5825_VerifyActivateDeactivateAndDeleteForWebhooks() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutoActiveDeleteWebhook_");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Anniversary Campaigns");
		try {

			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");

			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(false);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;

			String preferences2 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_KafkaInactive = Utilities.getPreferencesKeyValue(preferences2, "kafka_topics");

			Assert.assertFalse(keyValueFromPreferences_KafkaInactive.contains("marketing_notifications"),
					"marketing_notifications event is appearing in business kafka_topics preferences ");

			logger.info(
					"Verified that marketing_notifications events not appearing in business kafka_topics preferences");
			TestListeners.extentTest.get().info(
					"Verified that marketing_notifications events not appearing in business  kafka_topics preferences");

			String preferences3 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_marketing_eventsInActive = Utilities.getPreferencesKeyValue(preferences3, "marketing_events");
			Assert.assertFalse(keyValueFromPreferences_marketing_eventsInActive.contains("anniversary_campaign"),
					"anniversary_campaign event is appearing in business marketing_events preferences ");

			logger.info(
					"Verified that anniversary_campaign events  not appearing in business marketing_events preferences");
			TestListeners.extentTest.get().info(
					"Verified that anniversary_campaign events  not appearing in business marketing_events preferences");

			// deactivate the webhook and check events are not coming in business
			// preferences
			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

			// check webhook is active or not on Ui

			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");

			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("marketing_notifications"),
					"marketing_notifications event is not appearing in business kafka_topics preferences ");

			logger.info("Verified that marketing_notifications events are coming in business kafka_topics preferences");
			TestListeners.extentTest.get().info(
					"Verified that marketing_notifications events are coming in business kafka_topics preferences");

			String preferences1 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_marketing_events = Utilities.getPreferencesKeyValue(preferences1, "marketing_events");
			Assert.assertTrue(keyValueFromPreferences_marketing_events.contains("anniversary_campaign"),
					"anniversary_campaign event is not appearing in business marketing_events preferences ");

			logger.info("Verified that anniversary_campaign events are coming in business preferences");
			TestListeners.extentTest.get()
					.info("Verified that anniversary_campaign events are coming in business preferences");

		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().fail(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {

			// delete the webhook and check events are not coming in business preferences
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

				String preferences4 = DBUtils.executeQueryAndGetColumnValue(env, query1,
						"preferences");

				List<String> keyValueFromPreferences_KafkaInactive4 = Utilities
						.getPreferencesKeyValue(preferences4, "kafka_topics");

				Assert.assertTrue(!keyValueFromPreferences_KafkaInactive4.contains("marketing_notifications"),
						"marketing_notifications event is appearing in business kafka_topics preferences after deletion");

				logger.info(
						"Verified that marketing_notifications events not appearing in business kafka_topics preferences after deletion");
				TestListeners.extentTest.get().info(
						"Verified that marketing_notifications events not appearing in business  kafka_topics preferences after deletion");

				String preferences5 = DBUtils.executeQueryAndGetColumnValue(env, query1,
						"preferences");
				List<String> keyValueFromPreferences_marketing_eventsInActive5 = Utilities
						.getPreferencesKeyValue(preferences5, "marketing_events");
				Assert.assertTrue(!keyValueFromPreferences_marketing_eventsInActive5.contains("anniversary_campaign"),
						"anniversary_campaign event is appearing in business marketing_events preferences ");

				logger.info(
						"Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
				TestListeners.extentTest.get().info(
						"Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
			}
		}

	}// end of test case T5825_VerifyActivateDeactivateAndDeleteForWebhooks
	//automationtwentytwo
	@Test(groups = {"regression", "dailyrun" },priority = 5, description = "SQ-T5825 [EF - Custom Webhook] Event syncing on Webhook Activation, Deactivation and Deletion (Step 8)")
	public void T5825_VerifyActivateDeactivateAndDeleteForWebhooksPartTwo() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutoActiveDeleteWebhook_");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("POS Scanner Checkin");
		try {

			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(false);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;

			String preferences2 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_KafkaInactive = Utilities.getPreferencesKeyValue(preferences2, "kafka_topics");

			Assert.assertTrue(!keyValueFromPreferences_KafkaInactive.contains("marketing_notifications"),
					"marketing_notifications event is appearing in business kafka_topics preferences ");

			logger.info(
					"Verified that marketing_notifications events not appearing in business kafka_topics preferences");
			TestListeners.extentTest.get().info(
					"Verified that marketing_notifications events not appearing in business  kafka_topics preferences");

			String preferences3 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_transactionEvents = Utilities.getPreferencesKeyValue(preferences3, "transactional_events");
			Assert.assertFalse(keyValueFromPreferences_transactionEvents.contains("pos_scanner_checkin"),
					"pos_scanner_checkin event is not appearing in business marketing_events preferences ");

			logger.info(
					"Verified that pos_scanner_checkin events  not appearing in business marketing_events preferences");
			TestListeners.extentTest.get().info(
					"Verified that pos_scanner_checkin events  not appearing in business marketing_events preferences");

			// deactivate the webhook and check events are not coming in business
			// preferences
			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

			// check webhook is active or not on Ui

			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");

			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("transactional_notifications"),
					"transactional_notifications event is not appearing in business kafka_topics preferences ");

			logger.info("Verified that transactional_events events are coming in business kafka_topics preferences");
			TestListeners.extentTest.get()
					.info("Verified that transactional_events events are coming in business kafka_topics preferences");

			String preferences6 = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");
			List<String> keyValueFromPreferences_transactionEvents1 = Utilities.getPreferencesKeyValue(preferences6, "transactional_events");
			Assert.assertTrue(keyValueFromPreferences_transactionEvents1.contains("pos_scanner_checkin"),
					"pos_scanner_checkin event is not appearing in business marketing_events preferences ");

			logger.info("Verified that pos_scanner_checkin events are coming in business preferences");
			TestListeners.extentTest.get()
					.info("Verified that pos_scanner_checkin events are coming in business preferences");

		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().fail(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {

			// delete the webhook and check events are not coming in business preferences
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

				String preferences4 = DBUtils.executeQueryAndGetColumnValue(env, query1,
						"preferences");

				List<String> keyValueFromPreferences_KafkaInactive4 = Utilities
						.getPreferencesKeyValue(preferences4, "kafka_topics");

				Assert.assertTrue(!keyValueFromPreferences_KafkaInactive4.contains("marketing_notifications"),
						"marketing_notifications event is appearing in business kafka_topics preferences after deletion");

				logger.info(
						"Verified that marketing_notifications events not appearing in business kafka_topics preferences after deletion");
				TestListeners.extentTest.get().info(
						"Verified that marketing_notifications events not appearing in business  kafka_topics preferences after deletion");

				String preferences5 = DBUtils.executeQueryAndGetColumnValue(env, query1,
						"preferences");
				List<String> keyValueFromPreferences_marketing_eventsInActive5 = Utilities
						.getPreferencesKeyValue(preferences5, "marketing_events");
				Assert.assertTrue(!keyValueFromPreferences_marketing_eventsInActive5.contains("anniversary_campaign"),
						"anniversary_campaign event is appearing in business marketing_events preferences ");

				logger.info(
						"Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
				TestListeners.extentTest.get().info(
						"Verified that anniversary_campaign events  not appearing in business marketing_events preferences after deletion");
			}
		}

	}// end of test case T5825_VerifyActivateDeactivateAndDeleteForWebhooks
	//automationtwentytwo
	@Test(groups = {"regression", "dailyrun" },priority = 20, description = "SQ-T5824 [EF - Custom Webhook] Verify the Header in EF logs based on Header flag (Step-7)"
			+ "SQ-T5840 [EF - Events Visibility] Event Visibility in the Webhooks & Adapter tabs based on flags available in the Configurations Tabs")
	public void T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag_Part1() throws InterruptedException {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutomationTestWebhook_");
		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Loyalty Checkin");
		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false); 
		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Active Events",
				"Inactive Events");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Rewards", "Active Events",
				"Inactive Events");

		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
		pageObj.webhookManagerPage().clickOnCreateWebhookButton();
		boolean isLoyaltyCheckinEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Loyalty Checkin");

		Assert.assertFalse(isLoyaltyCheckinEventDisplayed,
				"Loyalty Checkin event is displaying in event selection drop down");
		logger.info("Verified that Loyalty Checkin event is not displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Loyalty Checkin event is not displaying in event selection drop down");

		boolean isRewardsEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Rewards");

		Assert.assertFalse(isRewardsEventDisplayed, "Rewards event is displaying in event selection drop down");
		logger.info("Verified that Rewards event is not displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Rewards event is not displaying in event selection drop down");

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Inactive Events",
				"Active Events");
		pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Rewards", "Inactive Events",
				"Active Events");
		pageObj.webhookManagerPage().clickOnSubmitButton();

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
		pageObj.webhookManagerPage().clickOnCreateWebhookButton();
		isLoyaltyCheckinEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Loyalty Checkin");

		Assert.assertTrue(isLoyaltyCheckinEventDisplayed,
				"Loyalty Checkin event is not displaying in event selection drop down");
		logger.info("Verified that Loyalty Checkin event is  displaying in event selection drop down");
		TestListeners.extentTest.get()
				.info("Verified that Loyalty Checkin event is  displaying in event selection drop down");

		isRewardsEventDisplayed = pageObj.webhookManagerPage()
				.verifyEventIsDisplayingInEventSelectionDropDown("Rewards");

		Assert.assertTrue(isRewardsEventDisplayed, "Rewards event is not displaying in event selection drop down");
		logger.info("Verified that Rewards event is  displaying in event selection drop down");
		TestListeners.extentTest.get().info("Verified that Rewards event is  displaying in event selection drop down");

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete
	//automationtwentytwo
	@Test(groups = {"regression", "dailyrun" },priority = 12, description = "SQ-T5824 [EF - Custom Webhook] Verify the Header in EF logs based on Header flag (Step-7)"
			+ "SQ-T5821 [EF - Custom Webhook] Verification of Guest, Loyalty Checkin & Gift Checkin Events and assertion in the Logs (Step-4.1)")
	public void T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag_Part2() throws InterruptedException {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String locationKey = dataSet.get("locationkey");
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = CreateDateTime.getUniqueString("AutomationTestWebhook_");
		try {
			List<String> eventNameList = new ArrayList<String>();
			eventNameList.add("Guest");
			eventNameList.add("Loyalty Checkin");
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			// generateBarcode
			pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
			pageObj.instanceDashboardPage().generateBarcode(dataSet.get("locationkey"));
			String barcode = pageObj.instanceDashboardPage().captureBarcode();

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activeOrInactiveEventsFromConfiguration("Loyalty Checkin", "Inactive Events",
					"Active Events");
			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Webhooks");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().clickOnCreateWebhookButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", webhookName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					webhookName + " Description");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Base URL",
					"Custom Webhook-1 Base URL (https://dashboard.staging.punchh.io)");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Webhook End Point", "/sidekiq");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Authentication", "None");
			pageObj.webhookManagerPage().selectEvent(eventNameList);
			pageObj.webhookManagerPage().clickOnActiveCheckBox(true);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			logger.info(webhookName + " webhook is created successfully");
			TestListeners.extentTest.get().info(webhookName + " webhook is created successfully");
			isWebhookCreatedSuccessfully = true;
			// create New User
			/// sign-up user
			userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
			logger.info("Api1 user signup is successful");
			TestListeners.extentTest.get().pass("Api1 user signup is successful");
			String access_token = signUpResponse.jsonPath().get("auth_token.token");
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			// checkin via APIV1
			Response checkinRes = pageObj.endpoints().Api1LoyaltyCheckinBarCode(dataSet.get("client"),
					dataSet.get("secret"), access_token, barcode);

			
			// Do checkin of 3
			Response checkinResponse1 = pageObj.endpoints().posCheckin(userEmail, locationKey, "50");
			Assert.assertEquals(checkinResponse1.getStatusCode(), 200,
					"Status code 200 did not match with POS Checkin ");
			logger.info("POS checkin is successful for " + "3" + " dollar");
			TestListeners.extentTest.get().pass("POS checkin is successful for " + "3" + " dollar");
			// checking logs for rewards
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
			
			String jsonObjectStrGuests = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,"",timeAfterEventTriggered);
			String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStrGuests, "event_name") ;
			
			Assert.assertEquals(actualEventName, "users", "user event is not matched in logs");
			logger.info("Verified that Guest event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guest event is coming in logs");
			
			String actualEmail=utils.findValueByKeyFromJsonAsString(jsonObjectStrGuests, "email") ;
			Assert.assertEquals(actualEmail , userEmail, userEmail+" Guest email value is not matching in logs");
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

			String jsonObjectStrLoyaltyCheckin = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Loyalty Checkin", webhookName,"",timeAfterEventTriggered);

			String actualEvenType=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "event_type") ;
			Assert.assertEquals(actualEvenType , "loyalty", "loyalty event is not coming in logs");
			logger.info("Verified that loyalty event is coming in logs");
			TestListeners.extentTest.get().info("Verified that loyalty event is coming in logs");
			
			String actualEmailInLoyaltyEvent=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "email") ;
			Assert.assertEquals(actualEmailInLoyaltyEvent , userEmail, userEmail+" emailid in loyalty event is not coming in logs");
			logger.info("Verified that Guests event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Guests event is coming in logs");

			String actualEventNameInLoyaltyEvent=utils.findValueByKeyFromJsonAsString(jsonObjectStrLoyaltyCheckin, "event_name") ;
			Assert.assertEquals(actualEventNameInLoyaltyEvent , "checkins", actualEventNameInLoyaltyEvent+"  loyalty event is not coming in logs");
			
			logger.info("Verified that Loyalty Checkin event is coming in logs");
			TestListeners.extentTest.get().info("Verified that Loyalty Checkin event is coming in logs");

		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {
			if (isWebhookCreatedSuccessfully) {
				pageObj.webhookManagerPage().deleteWebhook("Webhooks", webhookName, "Delete");
				logger.info(webhookName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(webhookName + " webhook is deleted successfully");

			}
		}

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete

	//Deltaco
	@Test(groups = {"regression", "dailyrun" },priority = 11 , description = "SQ-T5863[EF - Custom Webhook] Verification of Reward & Redemption Event (Step-4-2)")
	public void T5863_VerificationOfWebhookRewardAndRedemptionEvent() throws InterruptedException, HeadlessException, UnsupportedFlavorException, IOException {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String webhookName = dataSet.get("webhookName");
		String redeemable_id = dataSet.get("redeemable_id");
		String redeemableName = dataSet.get("redeemableName");
		String apiKey = dataSet.get("apiKey");

		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Rewards");
//		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

			// User SignUp using API
			userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
			String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
			String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			// Gift Reedemable to User
			Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, apiKey, "", redeemable_id);
			Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
					"Status code 201 did not matched for api2 send message to user");
			TestListeners.extentTest.get().pass("Api2  send reward reedemable to user is successful");

			// Get reward_id
			String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
					redeemable_id);
			logger.info("Reward id " + rewardId + " is generated successfully ");
			TestListeners.extentTest.get().info("Reward id " + rewardId + " is generated successfully ");

			// perform redemption second time
			Response posRedeem3 = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationkey"),
					rewardId);

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

			// Comment out the below code as it is not working properly -
			// checking logs for Guest event
			String jsonObjectStr =pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Redemption", webhookName,"\"redemption_type\":\"RewardRedemption\"",timeAfterEventTriggered);
				
			String actualEventName=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_name") ;
			
			Assert.assertEquals(actualEventName , "redemptions","Redemption event is not coming in logs");
			logger.info("Verified that Redemption event is coming in logs");
			TestListeners.extentTest.get().pass("Verified that Redemption event is coming in logs");
			
			String actualRedemptionType=utils.findValueByKeyFromJsonAsString(jsonObjectStr, "redemption_type") ;
			
			Assert.assertEquals(actualRedemptionType , "RewardRedemption","RewardRedemption redemption_type is not coming in logs");
			logger.info("Verified that RewardRedemption event is coming in logs");
			TestListeners.extentTest.get().pass("Verified that RewardRedemption event is coming in logs");

			String jsonObjectStr1  = 	pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Transactional Notifications", webhookName,"\"redeemable_name\":\""+redeemableName+"\"",timeAfterEventTriggered);
			String actualEventNameTransactional=utils.findValueByKeyFromJsonAsString(jsonObjectStr1, "event_name") ;


			Assert.assertEquals(actualEventNameTransactional , "transactional_notifications","transactional_notifications event is not coming in logs");
			logger.info("Verified that transactional_notifications event is coming in logs");
			TestListeners.extentTest.get().pass("Verified that transactional_notifications event is coming in logs");

			int actualredeemable_discount_amount= Integer.parseInt( utils.findValueByKeyFromJsonAsString(jsonObjectStr1, "redeemable_discount_amount")) ;

			
			Assert.assertTrue(actualredeemable_discount_amount==13, "Loyalty Checkin event is not coming in logs");
			logger.info(
					"Verified that transactional_notifications event > redeemable_discount_amount amount is coming in logs");
			TestListeners.extentTest.get().pass(
					"Verified that transactional_notifications event > redeemable_discount_amount amount is coming in logs");

//		} catch (Exception e) {
//			// TODO: handle exception
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		} 
//		finally {
//			logger.info("Executing finally block to Inactive the webhook");
//			TestListeners.extentTest.get().info("Executing finally block to Inactive the webhook");
//			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Inactive");
//			logger.info(webhookName + " webhook is inactive successfully");
//			TestListeners.extentTest.get().info(webhookName + " webhook is inactive successfully");
//
//		}

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete

	//automationtwentythree
	@Test(groups = {"regression", "dailyrun" },description = "SQ-T5839 [EF - Webhook, Adapter Tab] Visibility of Webhook & Adapter tabs in UI based on flags in Configurations Tab.", priority = 30)
	public void T5839_VisibilityOfWebhookAdapterTabsInUIBasedOnFlags() throws InterruptedException {

		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", false);
			//pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();
			boolean isWebhookTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Webhooks");
			Assert.assertFalse(isWebhookTabDisplayed, " Webhooks tab is displaying");
			logger.info("Verified that Webhooks tabs is not displaying");
			TestListeners.extentTest.get().pass("Verified that Webhooks tabs is not displaying");

//			boolean isAdaptersTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Adapters");
//			Assert.assertFalse(isAdaptersTabDisplayed, " Adapters tab is displaying");
//			logger.info("Verified that Adapters tabs is not displaying");
//			TestListeners.extentTest.get().pass("Verified that Adapters tabs is not displaying");

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", true);
//			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			boolean isWebhookTabDisplayed1 = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Webhooks");
			Assert.assertTrue(isWebhookTabDisplayed1, " Webhooks tab is displaying");
			logger.info("Verified that Webhooks tabs is displaying");
			TestListeners.extentTest.get().pass("Verified that Webhooks tabs is displaying");

//			isAdaptersTabDisplayed = pageObj.webhookManagerPage().verifyWebhookOrAdapterTabIsDisplaying("Adapters");
//			Assert.assertTrue(isAdaptersTabDisplayed, " Adapters tab is not displaying");
//			logger.info("Verified that Adapters tabs is  displaying");
//			TestListeners.extentTest.get().pass("Verified that Adapters tabs is displaying");
		}catch (AssertionError ae){
			logger.info(ae.getMessage());
			TestListeners.extentTest.get().info(ae.getMessage());
			Assert.fail(ae.getMessage());

		}catch (Exception e){
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		}
		finally {
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Webhooks Tab", true);
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Show Adapters Tab", true);
			pageObj.webhookManagerPage().clickOnSubmitButton();
		}

	} // end T5824_VerifyHeaderInEFLogsBasedOnHeaderFlag need to complete
	
	//Deltaco
	@Test(groups = {"regression", "dailyrun" },priority = 12, description = "SQ-T5864[EF - Custom Webhook] Verification of Coupon Issuance & Redemption (Step-4.3)")
	public void T5864_VerifyWebhookOfCouponIssuanceEvent() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = dataSet.get("webhookName");
		String redeemable_id = dataSet.get("redeemable_id");
		String redeemable_name = dataSet.get("redeemable_name");
		String apiKey = dataSet.get("apiKey");

		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Coupon Issuance");
		eventNameList.add("Coupon Redemption");
//		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");

			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("coupons"),
					"coupons event is not appearing in business preferences ");
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("user_coupon_redemptions"),
					"user_coupon_redemptions event is not appearing in business preferences ");
			logger.info("Verified coupons and user_coupon_redemptions events are coming in business preferences");
			TestListeners.extentTest.get()
					.pass("Verified coupons and user_coupon_redemptions events are coming in business preferences");

			// user signup then get the code coupon code
			userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
			logger.info("Api1 user signup is successful");
			TestListeners.extentTest.get().pass("Api1 user signup is successful");
			String access_token = signUpResponse.jsonPath().get("auth_token.token");
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
			// get coupon code

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
			Thread.sleep(5000);

			// Comment out the below code as it is not working properly -
			// checking logs for Guest event
			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Coupon Issuance",
					webhookName, "",timeAfterEventTriggered);

			String actualEventName = utils.findValueByKeyFromJsonAsString(jsonObjectStr.toString(), "event_name");

			Assert.assertEquals(actualEventName, "coupons", actualEventName + " Event name is not coupons");
			logger.info("Verified that coupons event is coming in logs");
			TestListeners.extentTest.get().info("Verified that coupons event is coming in logs");

			String couponCode = utils.findValueByKeyFromJsonAsString(jsonObjectStr.toString(), "code");

			logger.info(couponCode + " couponCode is generated in logs");
			TestListeners.extentTest.get().info(couponCode + " couponCode is generated in logs");

			String txn = "123456" + CreateDateTime.getTimeDateString();
			String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
			String key = CreateDateTime.getTimeDateString();
			Response respo = pageObj.endpoints().posRedemptionOfCouponCode(userEmail, date, couponCode, key, txn,
					dataSet.get("locationkey"));
			Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");

			logger.info("POS coupon code redemption is successful");
			TestListeners.extentTest.get().pass("POS coupon code redemption is successful");
			timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();

			String jsonObjectStr1 = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Coupon Redemption",
					webhookName, "\"code\":\"" + couponCode + "\"",timeAfterEventTriggered);

			String actualEventName1 = utils.findValueByKeyFromJsonAsString(jsonObjectStr1.toString(), "event_name");
			Assert.assertEquals(actualEventName1, "user_coupon_redemptions",
					actualEventName1 + " Event name is not user_coupon_redemptions");
			logger.info("Verified that user_coupon_redemptions event is coming in logs");
			TestListeners.extentTest.get().info("Verified that user_coupon_redemptions event is coming in logs");

			String couponCode1 = utils.findValueByKeyFromJsonAsString(jsonObjectStr1.toString(), "code");
			Assert.assertEquals(couponCode, couponCode1, couponCode1 + " couponCode is not same as previous one");

			logger.info(couponCode + " couponCode is generated in logs");
			TestListeners.extentTest.get().info(couponCode + " couponCode is generated in logs");

//		} catch (Exception e) {
//			// TODO: handle exception
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		} 
//		finally {
//			logger.info("Executing finally block to Inactive the webhook");
//			TestListeners.extentTest.get().info("Executing finally block to Inactive the webhook");
//			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Inactive");
//			logger.info(webhookName + " webhook is inactive successfully");
//			TestListeners.extentTest.get().info(webhookName + " webhook is inactive successfully");
//
//		}

	}//End of test
	
	//Deltaco
	@Test(groups = {"regression", "dailyrun" },priority = 12, description = "SQ-T5867 [EF - Custom Webhook] Verification of User Subscription Event (Step-4.6)")
	public void T5867_VerifyWebhookOfUserSubscriptionEvent() throws Exception {
		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String webhookName = dataSet.get("webhookName") ;
		String apiKey = dataSet.get("apiKey");
		String PlanName =  dataSet.get("subscriptionPlanName");
		String PlanID = dataSet.get("subscriptionPlanID");
		String spPrice = dataSet.get("spPrice");
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";

		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("User Subscription");
//		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");
			utils.longWaitInSeconds(4);
			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");
			
			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("user_subscriptions"),
					"user_subscriptions event is not appearing in business preferences ");
			
			logger.info("Verified user_subscriptions events are coming in business preferences");
			TestListeners.extentTest.get().pass("Verified user_subscriptions events are coming in business preferences");
			
			// user signup then get the code coupon code
			userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
			logger.info("Api1 user signup is successful");
			TestListeners.extentTest.get().pass("Api1 user signup is successful");
			String access_token = signUpResponse.jsonPath().get("auth_token.token");
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();

			
			pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
			pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
			
			pageObj.guestTimelinePage().messageGiftRewardsToUser("Gifting for Webhook event as subscription", "Subscription", PlanName,"");
			
			pageObj.guestTimelinePage().navigateToTabs("Subscriptions");
			int actualSubscriptionID = pageObj.guestTimelinePage().getGiftedSubscriptionID();
			logger.info(actualSubscriptionID + " subscription ID is generated after gifting to user");
			TestListeners.extentTest.get().info(actualSubscriptionID + " subscription ID is generated after gifting to user");
			
			pageObj.guestTimelinePage().clickOnSubscriptionCancel(dataSet.get("cancelType"));
			pageObj.guestTimelinePage().accecptSubscriptionCancellation("Price is Too High");
			logger.info("Subscription hard cancellation is successful");
			TestListeners.extentTest.get().info("Subscription hard cancellation is successful");
			
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook("", "User Subscription",
					webhookName, "\"subscription_id\":"+actualSubscriptionID , timeAfterEventTriggered);
			
			String actualEventName1 =utils.findValueByKeyFromJsonAsString(jsonObjectStr, "event_type") ; //      actBodyData1.getString("event_type").replace("[", "").replace("]", "");
			Assert.assertEquals("user_subscription_cancel", actualEventName1, actualEventName1 + " event_type name is not user_subscription_cancel");
			logger.info("Verified that user_subscription_cancel event is coming in logs");
			TestListeners.extentTest.get().info("Verified that user_subscription_cancel event is coming in logs");
			

//		} catch (Exception e) {
//			// TODO: handle exception
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		}
//		finally {
//			logger.info("Executing finally block to Inactive the webhook");
//			TestListeners.extentTest.get().info("Executing finally block to Inactive the webhook");
//			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Inactive");
//			logger.info(webhookName + " webhook is inactive successfully");
//			TestListeners.extentTest.get().info(webhookName + " webhook is inactive successfully");
//
//		}

	
	}//End of test
	
	//deltaco
	@Test(groups = {"regression", "dailyrun" },priority = 10, description = "SQ-T5868 [EF - Custom Webhook] Verification of Redeemable Event (Step-4.7)")
	public void T5868_VerifyWebhookOfRedeemableEvent() throws Exception {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String webhookName = dataSet.get("webhookName");

		List<String> eventNameList = new ArrayList<String>();
		eventNameList.add("Redeemable");
		String redeemableName = "AutomationRedeemableExistingQC_API_" + CreateDateTime.getTimeDateString();
		String redeemableExternalID = "";
//		try {
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Webhooks");
		  	pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Active");

			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities.getPreferencesKeyValue(preferences, "kafka_topics");

			Assert.assertTrue(keyValueFromPreferences_Kafka.contains("redeemables"),
					"redeemables event is not appearing in business preferences ");

			logger.info("Verified redeemables events are coming in business preferences");
			TestListeners.extentTest.get().pass("Verified redeemables events are coming in business preferences");

			// create redeemable

			String endTime = CreateDateTime.getFutureDateTimeInGivenFormate(1, "yyyy-MM-dd'T'HH:mm:ss");
			String expEndDateForUI = CreateDateTime.formatingISTIntoDesiredFormat(endTime);

			String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();

			Map<String, String> map = new HashMap<String, String>();
			map.put("name", QCName);
			map.put("redeemableName", redeemableName);
			map.put("external_id", "");
			map.put("amount_cap", "10.0");
			map.put("percentage_of_processed_amount", "1");
			map.put("qc_processing_function", "sum_amounts");
			map.put("line_item_selector_id", dataSet.get("lisExternalID"));
			map.put("locationID", null);
			map.put("external_id_redeemable", "");
			map.put("redeemableProcessingFunction", "Sum Of Amount");
			map.put("qualifier_type", "existing");
			map.put("amount_cap", "10.0");
			map.put("expQCName", dataSet.get("qcName"));
			map.put("end_time", endTime);
			map.put("redeeming_criterion_id", "InvalidQCExternalID");
			map.put("indefinetely", "true");
			map.put("lineitemSelector", dataSet.get("lineItemSelector"));
			// Added redeemable with existing QC
			map.put("redeeming_criterion_id", dataSet.get("qcExternalID"));
			// SQ-T5501 Create Redeemable with flat discount
			String redeemableNameWithFlatDiscount = "AutomationRedeemableFlatDiscount_API_"
					+ CreateDateTime.getTimeDateString();
			map.put("qualifier_type", "flat_discount");
			map.put("discount_amount", "230.0");
			map.put("redeemableName", redeemableNameWithFlatDiscount);
			map.put("end_time", null);
			map.put("expiry_days", "2");

			Response responseFlatDiscount = pageObj.endpoints().createRedeemableUsingAPI(dataSet.get("apiKey"), map);
			Assert.assertEquals(responseFlatDiscount.getStatusCode(), 200);
			logger.info(redeemableNameWithFlatDiscount + " redeemable is created successfully");
			TestListeners.extentTest.get().pass(redeemableNameWithFlatDiscount + " redeemable is created successfully");

			redeemableExternalID = responseFlatDiscount.jsonPath().getString("results[0].external_id").replace("]", "")
					.replace("[", "");
			
			String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

			String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook("", "Redeemable", webhookName,"\"name\":\"" + redeemableNameWithFlatDiscount + "\"" ,timeAfterEventTriggered);
			Assert.assertTrue(jsonObjectStr != null, "Redeemable event is not coming in logs");
			Assert.assertTrue(jsonObjectStr.contains("\"event_name\":\"redeemables\""),
					"Redeemable event is not coming in logs");

//		} catch (Exception e) {
//			// TODO: handle exception
//			logger.info(e.getMessage());
//			TestListeners.extentTest.get().info(e.getMessage());
//			Assert.fail(e.getMessage());
//		} finally {
			if (redeemableExternalID != null) {
				String deleteRedeemableQuery1 = deleteRedeemableQuery
						.replace("$redeemableExternalID", redeemableExternalID)
						.replace("$businessID", dataSet.get("business_id"));

				boolean isRedeemableDeleted = DBUtils.executeQuery(env, deleteRedeemableQuery1);
				Assert.assertTrue(isRedeemableDeleted, redeemableName + " redeemable is not deleted");
				logger.info(redeemableName + " redeemable is deleted successfully");
				TestListeners.extentTest.get().pass(redeemableName + " redeemable is deleted successfully");
			}
//			logger.info("Executing finally block to Inactive the webhook");
//			TestListeners.extentTest.get().info("Executing finally block to Inactive the webhook");
//			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Webhooks", webhookName, "Inactive");
//			logger.info(webhookName + " webhook is inactive successfully");
//			TestListeners.extentTest.get().info(webhookName + " webhook is inactive successfully");

		}

//	}
	//deltaco fixed
		@Test(groups = {"regression", "dailyrun" },priority = 3, description = "SQ-T6265 [EF - Error Codes] Webhook Error Verifications through Custom Webhook")
		public void T6265_ValidateWebhookErrorsThroughCustomWebhookTest() throws Exception {
			String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
					+ dataSet.get("business_id") + "';";

			Map<String, Integer> webhookNameInfoListmap = new HashMap<String, Integer>();
			webhookNameInfoListmap.put("DoNotDelete_TestWebhook401_Error", 401);
			webhookNameInfoListmap.put("DoNotDelete_TestWebhook408TransientError", 408);
			webhookNameInfoListmap.put("DoNotDelete_TestWebhook408_Error", 408);
			webhookNameInfoListmap.put("DoNotDelete_TestWebhook429_Error", 429);
			webhookNameInfoListmap.put("DoNotDelete_TestWebhook500_Error", 500);

			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Webhooks Management?", "check");

			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");

			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			// create New User
			String userEmail = pageObj.iframeSingUpPage().generateEmail();
			Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
					dataSet.get("secret"));
			String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

			// checking logs for Guest event

			for (Map.Entry<String, Integer> entry : webhookNameInfoListmap.entrySet()) {
				String webhookName = entry.getKey();
				int expectedErrorCode = entry.getValue();
				String jsonObjectStr = pageObj.webhookManagerPage().openLogsForWebhook(userEmail, "Guest", webhookName,
						"", timeAfterEventTriggered);

				int actualEventName = Integer
						.parseInt(utils.findValueByKeyFromJsonAsString(jsonObjectStr, "WebhookStatusCode"));
				Assert.assertEquals(actualEventName, expectedErrorCode, actualEventName
						+ "Guest event is not coming in logs with expected status code: " + expectedErrorCode);
				logger.info("Verified that webhook " + webhookName
						+ " is triggered for Guest event is coming in logs with status code: " + expectedErrorCode);
				TestListeners.extentTest.get().info("Verified that webhook " + webhookName
						+ " is triggered for Guest event is coming in logs with status code: " + expectedErrorCode);

			} // end of for loop

		}
	
	@AfterMethod
	public void afterClass() {
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
