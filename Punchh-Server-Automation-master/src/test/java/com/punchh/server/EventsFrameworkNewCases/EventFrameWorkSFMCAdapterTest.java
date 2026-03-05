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
public class EventFrameWorkSFMCAdapterTest {
	static Logger logger = LogManager.getLogger(EventFrameWorkSFMCAdapterTest.class);
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
			.ofEntries(Map.entry("Guest_update", Arrays.asList("updated_at", "account_balance_user_id", "gender")),
					Map.entry("Guest_delete", Arrays.asList("updated_at", "account_balance_user_id", "gender")),
					Map.entry("Guest_create", Arrays.asList("account_balance_user_id", "gender", "updated_at",
							"preferred_locale", "current_sign_in_ip", "current_sign_in_at", "joined_at", "user_id",
							"marketing_pn_subscription", "birthday", "city", "marketing_email_subscription",
							"last_sign_in_ip", "created_at", "favourite_store_numbers", "sms_subscription",
							"unsubscribe_reason", "last_sign_in_at", "last_activity_at", "fav_locations", "time_zone",
							"unsubscribed", "external_source_id", "last_user_agent", "business_id")),
					Map.entry("Loyalty Checkin_create",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "unsubscribed")),
					Map.entry("Marketing Notifications_create",
							Arrays.asList("unsubscribe_reason", "unsubscribed", "account_balance_user_id",
									"marketing_email_subscription", "marketing_pn_subscription")),

					Map.entry("Rewards_create",
							Arrays.asList("banked_currency", "current_membership_level_name", "last_visit",
									"loyalty_points", "pending_points", "total_credits", "total_debits", "total_visits",
									"unbanked_points", "anniversary", "avatar_remote_url", "gender",
									"marketing_email_subscription", "account_balance_user_id", "unsubscribed",
									"marketing_pn_subscription")),

					Map.entry("Redemption_create",
							Arrays.asList("account_balance_user_id", "marketing_email_subscription",
									"marketing_pn_subscription", "gender", "unsubscribed")),
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

	@Test(description = "SQ-T6261 [Outbound - SFMC] Triggering & Data Verification of Guest Event"
			+ "SQ-T6262 [Outbound - SFMC] Triggering & Date Verification of Marketing Notification Event"
			+ "SQ-T6263 [Outbound - SFMC] Triggering & Data Verification of Checkin, Reward & Redemption Events")
	@Owner(name = "Shashank Sharma")
	public void T6261_VerifySFMCAdapterTriggeringEventDataVerificationTest() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		List<String> eventNameList = Arrays.asList(dataSet.get("eventNameList").split("/"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().activateDeactivateWebhookOrAdapter("Adapters", adapterName, "Active");
		pageObj.webhookManagerPage().deleteAllWebhookOrAdapter("Adapters");

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

		String email = "automationadapteruser_" + CreateDateTime.getTimeDateString() + "@punchh.com";

		logger.info("******** START - Processing event type: " + eventType + " with email: " + email);
		TestListeners.extentTest.get()
				.info("******** START - Processing event type: " + eventType + " with email: " + email);

		String payloadEventData = getBrazePayloadDataBasedOnEventType(
				METHODNAME_PAYLOAD.getOrDefault(updatedEventType, ""), email, eventActionType, "");
		Map<String, Object> eventsPayloadDetailsMap = new ConcurrentHashMap<>();

		String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();
		kafkaProducerUtility.sendMessageOnKafkaTopics(devBootstrapServers,
				Map.of(brazeTopic, List.of(payloadEventData)));
		String dispatchedPayload = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs(email, eventType,
				adapterName, "", timeAfterEventTriggered);
		Assert.assertFalse(dispatchedPayload.isEmpty(), "Payload not found in the logs");
		logger.info("Dispatched payload is displaying: ");
		TestListeners.extentTest.get().pass("Dispatched payload is displaying: ");

		eventsPayloadDetailsMap.clear();
		eventsPayloadDetailsMap.put("email", email);
		String resultString = utils.verifiedKeyValueDispatchedEventPayload(eventsPayloadDetailsMap, dispatchedPayload,
				keysToSkipList);

		Assert.assertFalse(resultString.contains("email"), "Key value mismatch found in the payload: " + resultString);

		logger.info("Verified that attributes is not coming for the event : " + eventType);
		TestListeners.extentTest.get().pass("Verified that attributes is not coming for the event : " + eventType);

		TestListeners.extentTest.get().info("******** END - Processing event type: " + eventType);

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
