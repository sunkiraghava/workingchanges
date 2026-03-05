package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.KafkaConsumerUtility;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class InboundWebhookKafkaBulkingConsumerTest {

	private static final Logger logger = LogManager.getLogger(InboundWebhookKafkaBulkingConsumerTest.class);
	public ApiUtils apiUtils;
	public PageObj pageObj;
	public String sTCName;
	public String env;
	public String run = "ui";
	public String baseUrl;
	public static Map<String, String> dataSet;
	public Utilities utils;

	private static final int ACCOUNT_ID = 6071;

	private static final List<String> KAFKA_TOPICS = Arrays.asList("mothership.aegaeon_inbound.global_inbound_dlq",
			"mothership.aegaeon_inbound.segment_users");

	@BeforeMethod
	public void beforeMethod(Method method) {
		sTCName = method.getName();
		apiUtils = new ApiUtils();
		pageObj = new PageObj();
		dataSet = new ConcurrentHashMap<>();

		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();

		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;

		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);

		utils = new Utilities();
		logger.info(sTCName + " ==>" + dataSet);
	}

	@DataProvider(name = "userProfilesProvider")
	public Object[][] provideUserProfiles() {

		List<Map<String, String>> userProfiles1 = Arrays.asList(
				createUser("email", "rohit.doraya+1@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405701", "455405701"),
				createUser("email", "rohit.doraya+2@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405702", "455405702"),
				createUser("email", "rohit.doraya+3@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405703", "455405703"),
				createUser("email", "rohit.doraya+4@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405704", "455405704"),
				createUser("email", "rohit.doraya+5@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405705", "455405705"));

		List<Map<String, String>> userProfiles2 = Arrays.asList(
				createUser("email", "rohit.doraya+6@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405706", "455405706"),
				createUser("email", "rohit.doraya+7@partech.com", "0", "MP Audience - 01", "add", "punchh_user_id_bulk",
						"455405707", "455405707"),
				createUser("email", "rohit.doraya+8@partech.com", "69005", "MP Audience - 01", "addd",
						"punchh_user_id_bulk", "455405708", "455405708"),
				createUser("abc", "rohit.doraya+9@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405709", "455405709"),
				createUser("email", "rohit.doraya+10@partech.com", "69005", "MP Audience - 01", "add",
						"punchh_user_id_bulk", "455405710", ""));

		// Return as single item: list of combinations
		List<List<Map<String, String>>> userProfilesList = Arrays.asList(userProfiles1, userProfiles2);

		return new Object[][] { { userProfilesList } };
	}

	@Test(dataProvider = "userProfilesProvider", description = "SQ-T6764 [mParticle Inbound] Verify the mParticle Inbound Webhooks with Bulking User Data", priority = 0)
	public void T6764_verifyInboundWebhookWithKafkaConsumerBulking(List<List<Map<String, String>>> profilesList)
			throws Exception {
		int accountId = 6071;
		List<String> sentMessageIds = new ArrayList<>();

		// Setup Kafka consumer
		KafkaConsumerUtility consumer = KafkaConsumerUtility.setupTestConsumer(dataSet.get("bootstrapServer"),
				KAFKA_TOPICS, ACCOUNT_ID);

		try {
			utils.longWaitInSeconds(10);

			// Send segment messages
			logger.info("Sending Segment User Bulk messages...");
			for (List<Map<String, String>> profiles : profilesList) {
				Response response = pageObj.endpoints().inboundSegmentBulkUserEventGenerate(dataSet.get("proxyUrl"),
						dataSet.get("hostUrl"), dataSet.get("apiKey"), accountId, profiles);
				Assert.assertEquals(response.getStatusCode(), 200, "Status code is not 200");

				sentMessageIds.add(response.jsonPath().getString("id"));
				logger.info("Successfully sent request with ID: " + response.jsonPath().getString("id"));
				utils.longWaitInSeconds(2);
			}

			// Final wait and poll
			utils.longWaitInSeconds(30);
			consumer.pollForMessages(ACCOUNT_ID, 5000);

			// Log received messages (updated for List<JSONObject>)
			Map<String, Map<String, List<JSONObject>>> allMessages = consumer.getAllMessages();
			TestListeners.extentTest.get().info("Total topics with messages: " + allMessages.size());

			for (Map.Entry<String, Map<String, List<JSONObject>>> topicEntry : allMessages.entrySet()) {
				String topic = topicEntry.getKey();
				Map<String, List<JSONObject>> messages = topicEntry.getValue();
				logger.info("Topic: " + topic + ", Message IDs: " + messages.size());
				if (topic.equals("mothership.aegaeon_inbound.segment_users")) {
					Assert.assertTrue(messages.size() == 2,
							"Segment users message count not matching with expected value of 2");
					TestListeners.extentTest.get().info("Segment users Messages are matching with expected value of 2");
				}
				if (topic.equals("mothership.aegaeon_inbound.global_inbound_dlq")) {
					Assert.assertTrue(messages.size() == 1, "DLQ message count not matching with expected value of 1");
					TestListeners.extentTest.get().info("DLQ Messages are matching with expected value of 1");
				}

				for (Map.Entry<String, List<JSONObject>> msgEntry : messages.entrySet()) {
					String msgId = msgEntry.getKey();
					List<JSONObject> msgList = msgEntry.getValue();
					logger.info("  Message ID: " + msgId + ", Total messages: " + msgList.size());
					if (msgId.equals("ae5312b6-3eee-420f-ab2d-dfab4369a4c3")) {
						Assert.assertTrue(
								msgList.get(0).getJSONObject("payload").getJSONArray("user_profiles").length() == 1,
								"Segment users message count not matching with expected value of 1");
					}
				}

			}

			// Verify all messages were received
			Map<String, Boolean> messageResults = consumer.verifyMessages(sentMessageIds);
			logger.info("Message verification results: " + messageResults);

			int found = 0;
			StringBuilder missingMessages = new StringBuilder("Missing message IDs: ");

			for (Map.Entry<String, Boolean> result : messageResults.entrySet()) {
				if (result.getValue()) {
					logger.info("✓ Found message with ID: " + result.getKey());
					found++;
				} else {
					logger.error("✗ Message not found with ID: " + result.getKey());
					missingMessages.append(result.getKey()).append(", ");
				}
			}

			Assert.assertEquals(found, sentMessageIds.size(), "Not all messages were found. Found " + found + " out of "
					+ sentMessageIds.size() + ". " + missingMessages.toString());

		} finally {
			consumer.close();
		}
	}

	private Map<String, String> createUser(String userIdentityType, String email, String audienceId,
			String audienceName, String action, String partnerType, String partnerValue, String mpid) {

		Map<String, String> user = new HashMap<>();
		user.put("user_identities.type", userIdentityType);
		user.put("user_identities.value", email);
		user.put("audiences.audience_id", audienceId);
		user.put("audiences.audience_name", audienceName);
		user.put("audiences.action", action);
		user.put("partner_identities.type", partnerType);
		user.put("partner_identities.value", partnerValue);
		user.put("user_profile.mpid", mpid);
		return user;
	}

	@AfterMethod
	public void afterClass() {
		dataSet.clear();
		logger.info("Test data cleared");
	}
}
