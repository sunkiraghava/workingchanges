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
public class EventFrameWorkBrazeAdapterTest {
	static Logger logger = LogManager.getLogger(EventFrameWorkBrazeAdapterTest.class);
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

	List<String> keysToSkipList_reward = Arrays.asList("banked_currency", "current_membership_level_name", "last_visit",
			"loyalty_points", "pending_points", "total_credits", "total_debits", "total_visits", "unbanked_points",
			"anniversary", "avatar_remote_url", "gender", "marketing_email_subscription", "account_balance_user_id",
			"unsubscribed", "marketing_pn_subscription");

	List<String> keysToSkipList_guest = Arrays.asList("banked_currency", "current_membership_level_name", "last_visit",
			"loyalty_points", "pending_points", "total_credits", "total_debits", "total_visits", "unbanked_points",
			"anniversary", "avatar_remote_url", "gender", "marketing_email_subscription", "account_balance_user_id",
			"unsubscribed", "marketing_pn_subscription", "last_sign_in_at", "last_sign_in_ip", "created_at",
			"current_sign_in_ip", "updated_at", "joined_at", "last_user_agent");

	List<String> keysToSkipList_marketingnotification = Arrays.asList("unsubscribe_reason", "unsubscribed",
			"account_balance_user_id", "marketing_email_subscription", "marketing_pn_subscription");
	List<String> keysToSkipList_redemptions = Arrays.asList("account_balance_user_id", "marketing_email_subscription",
			"marketing_pn_subscription", "gender", "unsubscribed");
	List<String> keysToSkipList_transactionalNotifications = Arrays.asList("account_balance_user_id",
			"marketing_email_subscription", "marketing_pn_subscription", "unsubscribed");
	List<String> keysToSkipList_giftCheckin = Arrays.asList("account_balance_user_id", "marketing_email_subscription",
			"marketing_pn_subscription", "unsubscribed");
	List<String> keysToSkipList_loyaltyCheckin = Arrays.asList("account_balance_user_id",
			"marketing_email_subscription", "marketing_pn_subscription", "unsubscribed");
	List<String> keysToSkipList_coupons = Arrays.asList("account_balance_user_id", "marketing_email_subscription",
			"marketing_pn_subscription", "unsubscribed");
	List<String> keysToSkipList_coupon_redemptions = Arrays.asList("account_balance_user_id",
			"marketing_email_subscription", "marketing_pn_subscription", "unsubscribed");

	String brazeTopic_reward = "mothership.punchh_server.rewards";
	String brazeTopic_guest = "mothership.punchh_server.users";
	String brazeTopic_marketingNotification = "mothership.punchh_server.marketing_notifications";
	String brazeTopic_redemptions = "mothership.punchh_server.redemptions";
	String brazeTopic_transactionalNotifications = "mothership.punchh_server.transactional_notifications";
	String brazeTopic_giftCheckin = "mothership.punchh_server.checkins.gift";
	String brazeTopic_loyaltyCheckin = "mothership.punchh_server.checkins";
	String brazeTopic_coupons = "mothership.punchh_server.coupons";
	String brazeTopic_coupon_redemptions = "mothership.punchh_server.user_coupon_redemptions";

	String brazeTopic = "";

	String payloadPath_reward = "payload.user";
	String payloadPath_guest = "payload";
	String payloadPath_marketingNotification = "payload.user";
	String payloadPath_redemptions = "payload.user";
	String payloadPath_transactionalNotifications = "payload";
	String payloadPath_giftCheckin = "payload.user";
	String payloadPath_loyaltyCheckin = "payload.user";
	String payloadPath_coupons = "payload.user";
	String payloadPath_coupon_redemptions = "payload.user";

	String payloadPath = "";

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

	@Test(description = "(SQ-T6039)[Outbound - Braze] Triggering of Reward Event without enabling the Delta Flag"
			+ "(SQ-T6041) [Outbound - Braze] Triggering of Marketing Notification Event without enabling the Delta Flag"
			+ "(SQ-T6040) [Outbound - Braze] Triggering of Redemption Events without enabling the Delta Flag"
			+ "(SQ-T6024)[Outbound - Braze] Triggering of Coupon Issuance and Redemption Events without enabling the Delta Attribute Flag"
			+ "(SQ-T6021) [Outbound - Braze] Triggering of Guest Event without enabling the Delta Flag"
			+ "(SQ-T6023)[Outbound - Braze] Triggering of Transactional Notifications Event without enabling the Delta Attribute Flag"
			+ "SQ-T6022 [Outbound - Braze] Triggering of Gift Checkin & Loyalty Checkin Events without enabling the Delta Flag")
	public void T6039_VerifyBrazeBasedTriggeringEventWithoutEnablingDeltaFlag() throws Exception {
		String adapterName = dataSet.get("adapterName");
		String payloadEventData = "";
		String devBootstrapServers = dataSet.get("devBootstrapServers");
		String[] eventNameArray = dataSet.get("eventNameList").split("/");
		String randomAppearingKey = "2022";
		// Convert array to ArrayList
		List<String> eventNameList = new ArrayList<>(Arrays.asList(eventNameArray));

		// Navigate to Business and select the business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Webhooks Manager", "Outbound");
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Configuration");
		pageObj.webhookManagerPage().clickOnConfigurationFlagUsingAPI("Configuration", "Enable Success Headers Logging",
				false);
		pageObj.webhookManagerPage().clickOnSubmitButton();
		pageObj.webhookManagerPage().navigateToInboundWebhookManagerTabNew("Logs");
		String email = "automationadapteruser_" + CreateDateTime.getTimeDateString() + "@punchh.com";

		for (String eventType : eventNameList) {
			Map<String, Object> redeemablesEventDetailsMap = new LinkedHashMap<String, Object>();

			switch (eventType) {

			case "Rewards":
				brazeTopic = brazeTopic_reward;
				payloadPath = payloadPath_reward;
				keysToSkipList = keysToSkipList_reward;
				payloadEventData = kafkaPayloadObj.getBrazeRewardPayload(email);
				redeemablesEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData, payloadPath, "");
				break;

			case "Guest":
				brazeTopic = brazeTopic_guest;
				payloadPath = payloadPath_guest;
				keysToSkipList = keysToSkipList_guest;
				payloadEventData = kafkaPayloadObj.getBrzeGuestEventPayload(email);
				redeemablesEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData, payloadPath, "");
				break;

			case "Marketing Notifications":
				brazeTopic = brazeTopic_marketingNotification;
				payloadPath = payloadPath_marketingNotification;
				keysToSkipList = keysToSkipList_marketingnotification;
				payloadEventData = kafkaPayloadObj.getBrazeMarketingNotificationPayload(email);
				break;

			case "Redemption":
				brazeTopic = brazeTopic_redemptions;
				payloadPath = payloadPath_redemptions;
				keysToSkipList = keysToSkipList_redemptions;
				payloadEventData = kafkaPayloadObj.getBrazeRedemptionsEventPayload(email);
				break;
			case "Transactional Notifications":
				brazeTopic = brazeTopic_transactionalNotifications;
				payloadPath = payloadPath_transactionalNotifications;
				keysToSkipList = keysToSkipList_transactionalNotifications;
				payloadEventData = kafkaPayloadObj.getBrazeTransactionalNotificationEventPayload(email);
				break;
			case "Gift Checkin":
				brazeTopic = brazeTopic_giftCheckin;
				payloadPath = payloadPath_giftCheckin;
				keysToSkipList = keysToSkipList_giftCheckin;
				payloadEventData = kafkaPayloadObj.getBrazeGiftCheckinpayload(email);
				break;
			case "Loyalty Checkin":
				brazeTopic = brazeTopic_loyaltyCheckin;
				payloadPath = payloadPath_loyaltyCheckin;
				keysToSkipList = keysToSkipList_loyaltyCheckin;
				payloadEventData = kafkaPayloadObj.getBrazeLoyaltyCheckinEventPayload(email);

				break;
			case "Coupon Issuance":
				brazeTopic = brazeTopic_coupons;
				payloadPath = payloadPath_coupons;
				keysToSkipList = keysToSkipList_coupons;
				payloadEventData = kafkaPayloadObj.getBrazeCouponIssuanceEventPayload(email);
				break;

			case "Coupon Redemption":
				brazeTopic = brazeTopic_coupon_redemptions;
				payloadPath = payloadPath_coupon_redemptions;
				keysToSkipList = keysToSkipList_coupon_redemptions;
				payloadEventData = kafkaPayloadObj.getBrazeCouponRedemptionsEventPayload(email);
				break;
			}

			redeemablesEventDetailsMap = utils.getJsonKeysValueFromJsonPayload(payloadEventData, payloadPath, "");
			redeemablesEventDetailsMap = utils.replaceMapKeysWithNewKeys(redeemablesEventDetailsMap, dataSet);
			redeemablesEventDetailsMap.remove(randomAppearingKey);
			String timeAfterEventTriggered = getCurrentTimeStampAfterEventTriggered();

			// Get the singleton instance of KafkaProducerUtility
			KafkaProducerUtility kafkaProducerUtility = KafkaProducerUtility.getInstance(devBootstrapServers);

			Map<String, List<String>> topicMessages = new HashMap<>();
			topicMessages.put(brazeTopic, Arrays.asList(payloadEventData));

			// Send messages to Kafka topics
			kafkaProducerUtility.sendMessages(topicMessages);

			String payloadForRedemptionsEventFromUI = pageObj.webhookManagerPage().getDispatchedEventPayloadFromLogs("",
					eventType, adapterName, "", timeAfterEventTriggered);
			Assert.assertTrue(!payloadForRedemptionsEventFromUI.equals(""), "Payload not found in the logs");
			logger.info("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());
			TestListeners.extentTest.get()
					.pass("Dispatched Event Payload: " + payloadForRedemptionsEventFromUI.toString());

			String resultString = verifiedKeyValueDispatchedEventPayload(redeemablesEventDetailsMap,
					payloadForRedemptionsEventFromUI, keysToSkipList);
			Assert.assertTrue(resultString.equals(""), "Key value mismatch found in the payload: " + resultString);
			logger.info("****************Verified all keys in the payload for the event type: " + eventType);
			TestListeners.extentTest.get()
					.pass("****************Verified all keys in the payload for the event type: " + eventType);

		}

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

	public String verifiedKeyValueDispatchedEventPayload(Map<String, Object> flatMap, String payloadFromUI,
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

}
