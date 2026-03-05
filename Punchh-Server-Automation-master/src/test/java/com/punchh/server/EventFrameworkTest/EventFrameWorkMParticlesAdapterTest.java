package com.punchh.server.EventFrameworkTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.KafkaPayloads;
import com.punchh.server.pages.PageObj;
import io.restassured.response.Response;

//shashank sharma
@Listeners(TestListeners.class)
public class EventFrameWorkMParticlesAdapterTest {
	static Logger logger = LogManager.getLogger(EventFrameWorkMParticlesAdapterTest.class);
	public WebDriver driver;
	//String userEmail;
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private String segID = "11554400";
	private String segName = "Test MP Audience " + segID;
	private static Map<String, String> dataSet;
	Utilities utils;
	private String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='$businessID'";
	private KafkaPayloads kafkaPayloadObj = null;
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
		kafkaPayloadObj = new KafkaPayloads();
	}



	@Test(description = "(SQ-T5972) [Outbound - mParticle] mParticle Redeemable Event Data assertion when delta attribute is OFF in the business.")
	public void T5972_VerifyMParticleRedeemableEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String redeemablesTopic = dataSet.get("redeemablesTopic");
		Map<String, Object> redeemablesEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String gitftCheckinEventData = kafkaPayloadObj.getRedeemableEventPayload();
		redeemablesEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(gitftCheckinEventData, "payload.user" , "");
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(redeemablesTopic, Arrays.asList(gitftCheckinEventData));
  
		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs("", "Redeemable_update",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(redeemablesEventDetailsMap, payloadForRedemptionsEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5969_VerifyMParticleRedemptionEventDataValidation
	
	
	
	
	
	
	@Test(description = "SQ-T5968 [Outbound - mParticle] mParticle Gift Checkin Event Data assertion when delta attribute is OFF in the business.")
	public void TT5968_VerifyMParticleGiftCheckinEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationadapteruser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions","Punchh_account_balance_unsubscribe_reason");

		// Define topics and messages
		String gitftCheckinTopic = dataSet.get("gitftCheckinTopic");
		Map<String, Object> gitftCheckinEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String gitftCheckinEventData = kafkaPayloadObj.getGiftCheckinPayload(email);
		gitftCheckinEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(gitftCheckinEventData, "payload.user.account_balance" , "Punchh_account_balance");
		logger.info("gitftCheckinEventDetailsMap-- {}", gitftCheckinEventDetailsMap);
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(gitftCheckinTopic, Arrays.asList(gitftCheckinEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Gift Checkin",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(gitftCheckinEventDetailsMap, payloadForRedemptionsEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5969_VerifyMParticleRedemptionEventDataValidation
	
	
	

	@Test(description = "SQ-T5969 [Outbound - mParticle] mParticle Redemption Event Data assertion when delta attribute is OFF in the business.")
	public void T5969_VerifyMParticleRedemptionEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationadapteruser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String redemptionsTopic = dataSet.get("redemptionsTopic");
		Map<String, Object> redemptionEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String redemptionEventData = kafkaPayloadObj.getRedemptionEventPayload(email);
		redemptionEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(redemptionEventData, "payload.user" , "Punchh");
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(redemptionsTopic, Arrays.asList(redemptionEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Redemption",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(redemptionEventDetailsMap, payloadForRedemptionsEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5969_VerifyMParticleRedemptionEventDataValidation
	
	


	@Test(description = "SQ-T5971 [Outbound - mParticle] mParticle Transactional Notifications Event Data assertion when delta attribute is OFF in the business.")
	public void T5971_VerifyMParticleTransactionalNotificationsEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationadapteruser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String transactionalNotificationsTopic = dataSet.get("transactionalNotificationsTopic");
		Map<String, Object> transactionalNotificationsEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String transactionalNotificationsEventData = kafkaPayloadObj.getTransactionalPayload(email);
		transactionalNotificationsEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(transactionalNotificationsEventData, "payload" , "Punchh");
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(transactionalNotificationsTopic, Arrays.asList(transactionalNotificationsEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadFortransactionalNotificationsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Transactional Notifications",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadFortransactionalNotificationsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadFortransactionalNotificationsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadFortransactionalNotificationsEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(transactionalNotificationsEventDetailsMap, payloadFortransactionalNotificationsEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5971_VerifyMParticleTransactionalNotificationsEventDataValidation
	
	
	
	@Test(description = "SQ-T5966 [Outbound - mParticle] mParticle Reward Event Data assertion when delta attribute is OFF in the business.")
	public void T5967_VerifyMParticleLoyaltyCheckinEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationadapteruser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String loyaltyCheckinTopic = dataSet.get("loyaltyCheckinTopic");
		Map<String, Object> loyaltyCheckinEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String loayltyCheckinEventData = kafkaPayloadObj.getPayload(email);     // kafkaPayloadObj.getLoayltyCheckinEventPayload(userEmail, userID);
		loyaltyCheckinEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(loayltyCheckinEventData, "payload.user" , "Punchh");
		//String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(loyaltyCheckinTopic, Arrays.asList(loayltyCheckinEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForLoyaltyCheckinEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Loyalty Checkin",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForLoyaltyCheckinEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForLoyaltyCheckinEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForLoyaltyCheckinEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(loyaltyCheckinEventDetailsMap, payloadForLoyaltyCheckinEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5967_VerifyMParticleLoyaltyChekcinEventDataValidation
	
	

	@Test(description = "SQ-T5966 [Outbound - mParticle] mParticle Reward Event Data assertion when delta attribute is OFF in the business.")
	public void T5966_VerifyMParticleRewardEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationWebhookuser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		String userID = CreateDateTime.getTimeDateString();
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String rewardTopic = dataSet.get("rewardTopic");
		Map<String, Object> rewardEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String userEventData = kafkaPayloadObj.getCreateUserOnKafkaPayload(email,userID);
		String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();

		String rewardEventData = kafkaPayloadObj.createRewardEventPayload(email, userID);
		rewardEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(rewardEventData, "payload.user" , "Punchh");
		logger.info("rewardEventDetailsMap: "+rewardEventDetailsMap);

		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put("mothership.punchh_server.rewards", Arrays.asList(rewardEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRewardsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Rewards",
				adapterName, "", timeAfterEventTriggered);
		String finalVerifiedString = null;
		String resultString =  verifiedKeyValuemDispatchedEventPayload(rewardEventDetailsMap, payloadForRewardsEventFromUI,keysToSkipList);
		
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5966_VerifyMParticleRewardEventDataValidation
	
	
	@Test(description = "SQ-T5965 [Outbound - mParticle] mParticle Guest Event Data assertion when delta attribute is OFF in the business")
	public void T5965_VerifyMParticleGuestEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationWebhookuser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		String userID = CreateDateTime.getTimeDateString();
		List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String userTopic = dataSet.get("userTopic");
	
		// Define topics and messages
		Map<String, Object> guestEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		String userEventData = kafkaPayloadObj.getCreateUserOnKafkaPayload(email,userID);
		String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();

		guestEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(userEventData, "payload" , "Punchh");

		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(userTopic, Arrays.asList(userEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForGuestEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Guest",
				adapterName, "", timeAfterEventTriggered);
		String finalVerifiedString = null;
		String resultString =  verifiedKeyValuemDispatchedEventPayload(guestEventDetailsMap, payloadForGuestEventFromUI,keysToSkipList);
		
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5965_VerifyMParticleGuestEventDataValidation
	
	

	@Test(groups = {"regression", "dailyrun" },priority = 9, description = "SQ-T5841 [EF - mParticle] mParticle Adapter Create, Update & Active/Inactive Actions with Event Sync")
	public void T5841_ValidateMParticleAdapterCreateUpdateActiveInactiveActionsWithEventSync() throws Exception {

		String query1 = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		boolean isWebhookCreatedSuccessfully = false;
		String adapterName = CreateDateTime.getUniqueString("AutoTestAdapter_");
		try {
			List<String> eventNameList = new ArrayList<String>();
			eventNameList.add("Rewards");
			eventNameList.add("Redemption");
			// Navigate to Business and select the business
			pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
			pageObj.instanceDashboardPage().loginToInstance();
			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
			pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
			pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration",
					"Enable Success Headers Logging", false);
			pageObj.webhookManagerPage().clickOnSubmitButton();

			pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Adapters");
			pageObj.webhookManagerPage().clickOnCreateAdapterButton();

			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Name", adapterName);
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Description",
					adapterName + " Description");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("Admin Email",
					"superadmin4@example.com");

			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("Adapter", "mParticle");
			pageObj.webhookManagerPage().selectValueFromDropDownOnWebhookCreatePage("mParticle environment",
					"development");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("mParticle API key",
					"us1-a3a01d59f2d2784791d09b1f29ccaf51");
			pageObj.webhookManagerPage().enterValueInInputBoxOnWebhookCreatePage("mParticle Secret",
					"oAGzEwyAD6jLqnT577mT8eji3YQviuUl7Tq9vsvnqWqN-EJmQMkFVBlCJcbIqK7p");
			pageObj.webhookManagerPage().selectEvent(eventNameList);

			pageObj.webhookManagerPage().clickOnConfigurationFlag("Active", false);

			pageObj.webhookManagerPage().clickOnSubmitButton();
			pageObj.webhookManagerPage().clickOnCloseButtonForAdapter();
			isWebhookCreatedSuccessfully = true;
			utils.longWaitInSeconds(4);
			String preferences = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_Kafka = Utilities
					.getPreferencesKeyValue(preferences, "kafka_topics");
			Assert.assertFalse(keyValueFromPreferences_Kafka.contains("rewards"),
					"Rewards event is appearing in business preferences ");

			Assert.assertFalse(keyValueFromPreferences_Kafka.contains("redemptions"),
					"Redemptions event is appearing in business preferences ");

			Thread.sleep(4000);

			pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Adapters", adapterName, "Active");
			// active the adapter and check events are coming in business preferences

			String preferencesForActive = DBUtils.executeQueryAndGetColumnValue(env, query1,
					"preferences");

			List<String> keyValueFromPreferences_KafkaActive = Utilities
					.getPreferencesKeyValue(preferencesForActive, "kafka_topics");
			Assert.assertTrue(keyValueFromPreferences_KafkaActive.contains("rewards"),
					"Rewards event is not appearing in business preferences ");

			Assert.assertTrue(keyValueFromPreferences_KafkaActive.contains("redemptions"),
					"Redemptions event is not appearing in business preferences ");

		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
			Assert.fail(e.getMessage());
		} finally {
			if (isWebhookCreatedSuccessfully) {
				logger.info(adapterName + " webhook/adapter is deleting ");
				TestListeners.extentTest.get().info(adapterName + " webhook/adapter is deleting ");

			//	pageObj.webhookManagerPage().deleteWebhook("Adapters", adapterName, "Delete");
				logger.info(adapterName + " webhook is deleted successfully");
				TestListeners.extentTest.get().info(adapterName + " webhook is deleted successfully");

			}
		}

	}// end of test case
		// T5841_ValidateMParticleAdapterCreateUpdateActiveInactiveActionsWithEventSync

	
	@Test(description = "SQ-T5970 [Outbound - mParticle] mParticle Marketing Notifications Event Data assertion when delta attribute is OFF in the business.")
	public void T5970_VerifyMParticlePostCheckinEventMarketingNotificationDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String email = "automationadapteruser_"+CreateDateTime.getTimeDateString()+"@punchh.com";
		
		 List<String> keysToSkipList = Arrays.asList("Punchh_account_balance_user_id", "Punchh_external_source_id","Punchh_terms_and_conditions");

		// Define topics and messages
		String marketNotificationTopic = dataSet.get("marketNotificationTopic");
		Map<String, Object> marketNotificationTopicEventDetailsMap = new LinkedHashMap<String, Object>();

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		
		String gitftCheckinEventData = kafkaPayloadObj.getPostCheckinPayload(email);
		marketNotificationTopicEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(gitftCheckinEventData, "payload.user" , "Punchh");
		String timeAfterEventTriggered =  getCurrentTimeStampAfterEventTriggered();
		
		// Get the singleton instance of KafkaProducerUtility
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(marketNotificationTopic, Arrays.asList(gitftCheckinEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, "Gift Checkin",
				adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

		String resultString =  verifiedKeyValuemDispatchedEventPayload(marketNotificationTopicEventDetailsMap, payloadForRedemptionsEventFromUI,keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

	}//end of T5969_VerifyMParticleRedemptionEventDataValidation
	
	
	
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
	public String verifiedKeyValuemDispatchedEventPayload(Map<String, Object> flatMap, String payloadFromUI, List<String> keysToSkip) {
	    String finalVerifiedString = "";
	    for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
	        String key = entry.getKey();
	        String expValue = entry.getValue().toString();

	        // Skip comparison for keys in the keysToSkip list
	        if (keysToSkip.contains(key)) {
	            logger.info("Skipping comparison for key: " + key);
	            TestListeners.extentTest.get().info("Skipping comparison for key: " + key);
	            continue;
	        }

	        String actualKeyValue = utils.findValueByKeyFromJsonAsString(payloadFromUI, key);
	        try {
	            if (!actualKeyValue.equalsIgnoreCase(expValue) || actualKeyValue == null) {
	                finalVerifiedString = finalVerifiedString + key + "/";
	                logger.info(
	                        "Key: " + key + " does not match. Expected: " + expValue + ", Actual: " + actualKeyValue);
	                TestListeners.extentTest.get().info(
	                        "Key: " + key + " does not match. Expected: " + expValue + ", Actual: " + actualKeyValue);

	            } else {
	                logger.info("Key: " + key + " matches actual and expected value: " + actualKeyValue);
	                TestListeners.extentTest.get()
	                        .info("Key: " + key + " matches actual and expected value: " + actualKeyValue);

	            }
	        } catch (Exception e) {
	            finalVerifiedString = finalVerifiedString + key + "/";
	            logger.info(key + " not found in the payload");
	            TestListeners.extentTest.get().fail(key + " not found in the payload");
	        }
	    }
	    return finalVerifiedString;
	}
	
}
