package com.punchh.server.EventFrameworkTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.KafkaPayloads;
import com.punchh.server.pages.PageObj;

//shashank sharma
@Listeners(TestListeners.class)
public class EventFrameWorkMParticlesAdapterTest_POC {
	static Logger logger = LogManager.getLogger(EventFrameWorkMParticlesAdapterTest_POC.class);
	public WebDriver driver;
	// String userEmail;
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	Utilities utils;
	private KafkaPayloads kafkaPayloadObj = null;
	List<String> keysToSkipList = new ArrayList<String>();

	private static final Map<String, String> MPARTICLES_TOPICS = Map.ofEntries(

			Map.entry("redeemable", "mothership.punchh_server.redeemables"),
			Map.entry("gift_checkin", "mothership.punchh_server.checkins.gift"));

	private static final Map<String, String> PAYLOAD_PATHS = Map.ofEntries(Map.entry("redeemable", "payload"),
			Map.entry("gift_checkin", "payload.user.account_balance"));

	private static final Map<String, List<String>> KEYS_TO_SKIP = Map.ofEntries(
			Map.entry("Redeemable_update", Arrays.asList("status", "points_required_to_redeem", "discount_amount")),
			Map.entry("Gift Checkin_create", Arrays.asList("account_balance_user_id", "marketing_email_subscription",
					"marketing_pn_subscription", "unsubscribed")));

	private static final Map<String, String> METHODNAME_PAYLOAD = Map.ofEntries(
			Map.entry("redeemable", "getRedeemableEventPayload"),
			Map.entry("gift_checkin", "getGiftCheckinPayload"));
	
	private static final Map<String, String> PREFIX_STRING = Map.ofEntries(
			Map.entry("redeemable", ""),
			Map.entry("gift_checkin", "Punchh_account_balance"));

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

	@Test(description = "(SQ-T5972) [Outbound - mParticle] mParticle Redeemable Event Data assertion when delta attribute is OFF in the business."
			+ "SQ-T5968 [Outbound - mParticle] mParticle Gift Checkin Event Data assertion when delta attribute is OFF in the business.")
	public void T5972_VerifyMParticleEventDataValidation() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		List<String> eventNameList = Arrays.asList(dataSet.get("eventNameList").split("/"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
//		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
//				false);
//		pageObj.webhookManagerPage().clickOnSubmitButton();
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

		for (String eventType : eventNameList) {
			processEventType(eventType, adapterName, devBootstrapServers);
		}
	}

//	@AfterMethod
	public void afterClass() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

	private void processEventType(String eventType, String adapterName, String devBootstrapServers) throws Exception {
		System.out.println("Processing event type: " + eventType);
		String[] eventTypeArray = eventType.split("_");
		String updatedEventType = eventTypeArray[0].replace(" ", "_").toLowerCase();
		String eventActionType = eventTypeArray[1];

		List<String> previousChangesKeysList = utils.getPreviousChangesDataList(dataSet.get(eventType));
		String brazeTopic = MPARTICLES_TOPICS.getOrDefault(updatedEventType, "");
		String payloadPath = PAYLOAD_PATHS.getOrDefault(updatedEventType, "");
		String  prefixString = PREFIX_STRING.getOrDefault(updatedEventType, "");
		List<String> keysToSkipList = KEYS_TO_SKIP.getOrDefault(eventType, Collections.emptyList());
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		String email = "automationadapteruser_" + CreateDateTime.getTimeDateString() + "@punchh.com";

		logger.info("******** START - Processing event type: " + eventType + " with email: " + email);
		TestListeners.extentTest.get()
				.info("******** START - Processing event type: " + eventType + " with email: " + email);

		String payloadEventData = getBrazePayloadDataBasedOnEventType(
				METHODNAME_PAYLOAD.getOrDefault(updatedEventType, ""), email, eventActionType, "");
		Map<String, Object> eventsPayloadDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData,
				payloadPath, prefixString);
		eventsPayloadDetailsMap = preparePayloadDetails(eventsPayloadDetailsMap, payloadEventData);

		Map<String, List<String>> topicMessages = new HashMap<>();
		topicMessages.put(brazeTopic, Arrays.asList(payloadEventData));

		// Send messages to Kafka topics
		kafkaProducerUtility.sendMessages(topicMessages);
		String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();

		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs("",
				eventType, adapterName, "", timeAfterEventTriggered);
		Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
		logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
		TestListeners.extentTest.get().info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

		String resultString = verifiedKeyValuemDispatchedEventPayload(eventsPayloadDetailsMap,
				payloadForRedemptionsEventFromUI, keysToSkipList);
		Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
		logger.info("Verified all keys in the payload");
		TestListeners.extentTest.get().pass("Verified all keys in the payload");

		logger.info("Verified that attributes is not coming for the event : " + eventType);
		TestListeners.extentTest.get().pass("Verified that attributes is not coming for the event : " + eventType);

		TestListeners.extentTest.get().info("******** END - Processing event type: " + eventType);

	}

	public String verifiedKeyValuemDispatchedEventPayload(Map<String, Object> flatMap, String payloadFromUI,
			List<String> keysToSkip) {
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

	private Map<String, Object> preparePayloadDetails(Map<String, Object> eventsPayloadDetailsMap,
			String payloadEventData) {
		List<String> previousChangesKeysList = utils.getPreviousChangesKeysListForAdapter(payloadEventData,
				"previous_changes");
		// Validate if the previous_changes key is not found
		if (previousChangesKeysList.size() != 0) {
			Map<String, Object> previousChangesDataSetWithValueMap = utils.filterMapByKeys(previousChangesKeysList,
					eventsPayloadDetailsMap);
			eventsPayloadDetailsMap.clear();
			eventsPayloadDetailsMap.putAll(previousChangesDataSetWithValueMap);
		}
		return eventsPayloadDetailsMap;
	}

	public String getCurrentTimeStampAfterEventTriggered() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss a");
		LocalDateTime now = LocalDateTime.now();
		String formatted = now.format(formatter);
		return formatted.toString();
	}

	// sequence of methods parameters for payload should in this order String
	// email,String eventActionType , String previousChanges
	public static String getBrazePayloadDataBasedOnEventType(String methodName, String email, String eventActionType,
			String previousChangesKey) {
		String payloadEventData = "";
		try {
			// Class containing the methods
			Class<?> clazz = KafkaPayloads.class;

			// Define possible parameter combinations
			Class<?>[][] parameterCombinations = { {}, { String.class, String.class, String.class },
					{ String.class, String.class }, { String.class } };

			Method method = null;

			// Iterate through parameter combinations
			for (Class<?>[] params : parameterCombinations) {
				method = getMethodDynamically(clazz, methodName, params);
				if (method != null) {
					logger.info("Method found with " + params.length + " parameters.");
					break;
				}
			}

			if (method == null) {
				throw new IllegalArgumentException("Method not found with the given parameter count.");
			}

			// Fetch the number of parameters
			int parameterCount = method.getParameterCount();
			logger.info("Number of parameters: " + parameterCount);

			// Prepare parameter values dynamically based on parameter count
			Object[] parameterValues = new Object[parameterCount];

			if (parameterCount == 0) {

			} else if (parameterCount == 1) {
				parameterValues[0] = email;
			} else if (parameterCount == 2) {
				parameterValues[0] = email; // Example value for the first parameter
				parameterValues[1] = eventActionType; // Example value for the second parameter
			} else if (parameterCount == 3) {
				parameterValues[0] = email; // Example value for the first parameter
				parameterValues[1] = eventActionType; // Example value for the second parameter
				parameterValues[2] = previousChangesKey; // Example value for the third parameter
			}

			// Create an instance of the class
			Object instance = clazz.getDeclaredConstructor().newInstance();

			// Invoke the method dynamically
			Object result = method.invoke(instance, parameterValues);
			payloadEventData = (String) result.toString();
			// Print the result
		} catch (Exception e) {
			e.printStackTrace();
		}
		return payloadEventData; // Return null or handle as needed
	}

	public static Method getMethodDynamically(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		Method method = null;
		try {
			method = clazz.getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			// Handle method not found
		}
		return method;
	}

}// end of class
