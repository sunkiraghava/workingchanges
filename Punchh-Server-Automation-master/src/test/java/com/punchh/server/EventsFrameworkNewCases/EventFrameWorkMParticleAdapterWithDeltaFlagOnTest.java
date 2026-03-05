package com.punchh.server.EventsFrameworkNewCases;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.KafkaPayloads;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//shashank sharma
@Listeners(TestListeners.class)
public class EventFrameWorkMParticleAdapterWithDeltaFlagOnTest {
	static Logger logger = LogManager.getLogger(EventFrameWorkMParticleAdapterWithDeltaFlagOnTest.class);
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

	private static final Map<String, String> BRAZE_TOPICS = Map.ofEntries(
			Map.entry("guest", "mothership.punchh_server.users"),
			Map.entry("rewards", "mothership.punchh_server.rewards"),
			Map.entry("marketing_notifications", "mothership.punchh_server.marketing_notifications"),
			Map.entry("redemption", "mothership.punchh_server.redemptions"),
			Map.entry("transactionalNotifications", "mothership.punchh_server.transactional_notifications"),
			Map.entry("gift_checkin", "mothership.punchh_server.checkins.gift"),
			Map.entry("loyalty_checkin", "mothership.punchh_server.checkins"),
			Map.entry("coupons", "mothership.punchh_server.coupons"),
			Map.entry("couponRedemptions", "mothership.punchh_server.user_coupon_redemptions"));

	private static final Map<String, String> PAYLOAD_PATHS = Map.ofEntries(Map.entry("guest", "payload"),
			Map.entry("rewards", "payload.user"), Map.entry("marketing_notifications", "payload.user"),
			Map.entry("redemption", "payload.user"), Map.entry("transactionalNotifications", "payload"),
			Map.entry("gift_checkin", "payload.user"), Map.entry("loyalty_checkin", "payload.user"),
			Map.entry("coupons", "payload.user"), Map.entry("couponRedemptions", "payload.user"));

	private static final Map<String, List<String>> KEYS_TO_SKIP = Map
			.ofEntries(Map.entry("Guest_update", Arrays.asList("Punchh_updated_at", "account_balance_user_id")),
					Map.entry("Guest_delete", Arrays.asList("updated_at", "account_balance_user_id", "gender")),
					Map.entry("Guest_create", Arrays.asList("Punchh_updated_at", "Punchh_external_source_id","Punchh_account_balance_user_id","Punchh_favourite_store_numbers","Punchh_terms_and_conditions","Punchh_fav_locations","Punchh_address_line1")),
					Map.entry("Loyalty Checkin_create",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "unsubscribed")),
					Map.entry("Marketing Notifications_create",
							Arrays.asList("Punchh_account_balance_total_point_credits","Punchh_anniversary","Punchh_account_balance_user_id","Punchh_first_name","Punchh_last_name","Punchh_user_status","Punchh_marketing_email_subscription","Punchh_guest_type","Punchh_birthday","Punchh_user_id","Punchh_account_balance_total_redeemable_visits","Punchh_unsubscribed","Punchh_account_balance_total_credits","Punchh_email","Punchh_account_balance_total_debits","Punchh_account_balance_unredeemed_cards","Punchh_last_activity_at","Punchh_account_balance_total_visits","Punchh_account_balance_net_balance","Punchh_account_balance_loyalty_points/Punchh_marketing_pn_subscription/Punchh_signup_channel/Punchh_sms_subscription/Punchh_account_balance_net_debits/Punchh_account_balance_total_lifetime_points/Punchh_external_source_id/Punchh_account_balance_pending_points/Punchh_account_balance_last_visit/\n"
									+ "")),

					Map.entry("Rewards_create",
							Arrays.asList("banked_currency", "current_membership_level_name", "last_visit",
									"loyalty_points", "pending_points", "total_credits", "total_debits", "total_visits",
									"unbanked_points", "anniversary", "avatar_remote_url", "gender",
									"marketing_email_subscription", "account_balance_user_id", "unsubscribed",
									"marketing_pn_subscription")),

					Map.entry("Redemption_create",
							Arrays.asList("Punchh_phone","Punchh_anniversary","Punchh_account_balance_user_id","Punchh_first_name","Punchh_last_name","Punchh_user_status","Punchh_marketing_email_subscription","Punchh_guest_type","Punchh_birthday","Punchh_gender","Punchh_user_id","Punchh_unsubscribed","Punchh_email","Punchh_marketing_pn_subscription","Punchh_signup_channel","Punchh_sms_subscription","Punchh_external_source_id")),
					Map.entry("transactionalNotifications",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "unsubscribed")),
					Map.entry("Gift Checkin_create",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "unsubscribed")),

					Map.entry("coupons",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "unsubscribed")),
					Map.entry("couponRedemptions", Arrays.asList("account_balance_user_id",
							"marketing_email_subscription", "marketing_pn_subscription", "unsubscribed")));

	private static final Map<String, String> METHODNAME_PAYLOAD = Map.ofEntries(
			Map.entry("guest", "getBrazeGuestCreateDeleteAndUpdatePayload"),
			Map.entry("marketing_notifications", "getBrazeMarketingNotificationPayload"),
			Map.entry("loyalty_checkin", "getBrazeLoyaltyCheckinEventPayload"),
			Map.entry("gift_checkin", "getBrazeGiftCheckinpayload"), Map.entry("rewards", "getBrazeRewardPayload"),
			Map.entry("redemption", "getBrazeRedemptionsEventPayload"));

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

	@Test(description = "(SQ-T6126) [Outbound - mParticle] Triggering of Guest Create & Delete Event by enabling the Delta Flag\n"

			+ "SQ-T6127 [Outbound - mParticle] Triggering of Guest Update Event by enabling the Delta Flag\n"

			+ "SQ-T6128 [Outbound - mParticle] Triggering of Marketing Notification Event by enabling the Delta Flag\n"

			+ "SQ-T6129 [Outbound - mParticle] Triggering of Checkin, Reward & Redemptions Events by enabling the Delta Flag\n"

			+ "SQ-T6130 [Outbound - mParticle] Triggering of Transactional Notifications Event by enabling the Delta Flag")
	@Owner(name = "Shashank Sharma")
public void T6126_VerifyMParticleAdapterBasedTriggeringEventWithEnablingDeltaFlag() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		List<String> eventNameList = Arrays.asList(dataSet.get("eventNameList").split("/"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Adapters", adapterName, "Active");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");

		for (String eventType : eventNameList) {
			processEventType(eventType, adapterName, devBootstrapServers);
		}
	}

	@AfterMethod
	public void afterClass() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

	private void processEventType(String eventType, String adapterName, String devBootstrapServers) throws Exception {
		String[] eventTypeArray = eventType.split("_");
		String updatedEventType = eventTypeArray[0].replace(" ", "_").toLowerCase();
		String eventActionType = eventTypeArray[1];

		List<String> previousChangesKeysList = utils.getPreviousChangesDataList(dataSet.get(eventType));
		String brazeTopic = BRAZE_TOPICS.getOrDefault(updatedEventType, "");
		String payloadPath = PAYLOAD_PATHS.getOrDefault(updatedEventType, "");
		List<String> keysToSkipList = KEYS_TO_SKIP.getOrDefault(eventType, Collections.emptyList());
		KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

		if (!previousChangesKeysList.isEmpty() || previousChangesKeysList.size() != 0) {

			for (String previousChangesKey : previousChangesKeysList) {
				TestListeners.extentTest.get().info(
						"******** START - Processing event type: " + eventType + " with key: " + previousChangesKey);
				String email = "automationadapteruser_" + CreateDateTime.getTimeDateString() + "@punchh.com";

				logger.info("Processing event type: " + updatedEventType + " with key: " + previousChangesKey
						+ " and email: " + email);

				String payloadEventData = getBrazePayloadDataBasedOnEventType(
						METHODNAME_PAYLOAD.getOrDefault(updatedEventType, ""), email, eventActionType,
						previousChangesKey);

				Map<String, Object> eventsPayloadDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData,
						payloadPath, "");

				eventsPayloadDetailsMap = preparePayloadDetails(eventsPayloadDetailsMap, payloadEventData,"Punchh_");
				System.out.println(
						"EventFrameWorkMParticleAdapterWithDeltaFlagOnTest.processEventType()::eventsPayloadDetailsMap"
								+ eventsPayloadDetailsMap);

				String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();
				kafkaProducerUtility.sendMessageOnKafkaTopics(devBootstrapServers,
						Map.of(brazeTopic, List.of(payloadEventData)));

				String dispatchedPayload = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email,
						eventType, adapterName, "", timeAfterEventTriggered);
				Assert.assertFalse(dispatchedPayload.isEmpty(), "Payload not found in the logs");

				String resultString = utils.verifiedKeyValueDispatchedEventPayload(eventsPayloadDetailsMap,
						dispatchedPayload, keysToSkipList);
				Assert.assertTrue(resultString.isEmpty(), "Key value mismatch found in the payload: " + resultString);

				logger.info("Verified all keys in the payload for event type: " + eventType);
				TestListeners.extentTest.get().pass("Verified all keys in the payload for event type: " + eventType);

				TestListeners.extentTest.get().info(
						"******** END - Processing event type: " + eventType + " with key: " + previousChangesKey);

			}
		} else {

			String email = "automationadapteruser_" + CreateDateTime.getTimeDateString() + "@punchh.com";

			logger.info("******** START - Processing event type: " + eventType + " with email: " + email);
			TestListeners.extentTest.get()
					.info("******** START - Processing event type: " + eventType + " with email: " + email);

			String payloadEventData = getBrazePayloadDataBasedOnEventType(
					METHODNAME_PAYLOAD.getOrDefault(updatedEventType, ""), email, eventActionType, "");
			System.out.println("payloadEventData -- " + payloadEventData);
			Map<String, Object> eventsPayloadDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData,
					payloadPath, "Punchh");
			//eventsPayloadDetailsMap = preparePayloadDetails(eventsPayloadDetailsMap, payloadEventData,"Punchh_");

			System.out.println("eventsPayloadDetailsMap -- "+ eventsPayloadDetailsMap);
			String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();
			kafkaProducerUtility.sendMessageOnKafkaTopics(devBootstrapServers,
					Map.of(brazeTopic, List.of(payloadEventData)));
			String dispatchedPayload = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, eventType,
					adapterName, "", timeAfterEventTriggered);
			Assert.assertFalse(dispatchedPayload.isEmpty(), "Payload not found in the logs");

			String resultString = utils.verifiedKeyValueDispatchedEventPayload(eventsPayloadDetailsMap,
					dispatchedPayload, keysToSkipList);
			System.out.println("resultString== "+resultString);

			Assert.assertTrue(resultString.isEmpty(),
					"Key value mismatch found in the payload: " + dispatchedPayload);

			logger.info("Verified that attributes is not coming for the event : " + eventType);
			TestListeners.extentTest.get().pass("Verified that attributes is not coming for the event : " + eventType);

			TestListeners.extentTest.get().info("******** END - Processing event type: " + eventType);

		}
	}

	private Map<String, Object> preparePayloadDetails(Map<String, Object> eventsPayloadDetailsMap,
			String payloadEventData, String keyPrefix) {
		List<String> previousChangesKeysList = utils.getPreviousChangesKeysListForAdapter(payloadEventData,
				"previous_changes");

// Validate if the previous_changes key is found
		if (!previousChangesKeysList.isEmpty()) {
			Map<String, Object> previousChangesDataSetWithValueMap = utils.filterMapByKeys(previousChangesKeysList,
					eventsPayloadDetailsMap);

// Create a new map with prefixed keys
			Map<String, Object> prefixedMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : previousChangesDataSetWithValueMap.entrySet()) {
				prefixedMap.put(keyPrefix + entry.getKey(), entry.getValue());
			}

// Replace original map contents with prefixed map
			eventsPayloadDetailsMap.clear();
			eventsPayloadDetailsMap.putAll(prefixedMap);
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
			Class<?>[][] parameterCombinations = { { String.class, String.class, String.class },
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
			if (parameterCount == 1) {
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
