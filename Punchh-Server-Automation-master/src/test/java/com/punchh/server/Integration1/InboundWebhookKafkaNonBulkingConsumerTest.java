package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class InboundWebhookKafkaNonBulkingConsumerTest {

    private static final Logger logger = LogManager.getLogger(InboundWebhookKafkaNonBulkingConsumerTest.class);
    public ApiUtils apiUtils;
    public PageObj pageObj;
    public String sTCName;
    public String env;
    public String run = "ui";
    public String baseUrl;
    public static Map<String, String> dataSet;
    public Utilities utils;
    private static final int ACCOUNT_ID = 6071;
    private static final int SEGMENT_ID = 69005;
    private static final String SEGMENT_NAME = "mParticle Audience - Test Segment";
    private static final String TEST_EMAIL = "rohit.doraya@partech.com";
    private static final String TEST_MPID = "434567890";
    private static final String BIZ_NAME = "My Rewards";


    private static final List<String> KAFKA_TOPICS = Arrays.asList(
            //"mothership.aegaeon_inbound.global_inbound_dlq",
    		"mothership.aegaeon_inbound.inbound_dlq",
            "mothership.aegaeon_inbound.segments",
            "mothership.aegaeon_inbound.segment_users"
    );

    @BeforeMethod
    public void beforeMethod(Method method) {
        sTCName = method.getName();
        apiUtils = new ApiUtils();
        pageObj = new PageObj();
        dataSet = new ConcurrentHashMap<>();

        env = pageObj.getEnvDetails().setEnv();
        baseUrl = pageObj.getEnvDetails().setBaseUrl();

        pageObj.readData().ReadDataFromJsonFile(
                pageObj.readData().getJsonFilePath(run, env), sTCName);
        dataSet = pageObj.readData().readTestData;

        pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
                pageObj.readData().getJsonFilePath(run, env, "Secrets"),
                dataSet.get("slug"));
        dataSet.putAll(pageObj.readData().readTestData);

        utils = new Utilities();
        logger.info(sTCName + " ==>" + dataSet);
    }

    /**
     * DataProvider returns lists of segment events and segment user events.
     * Uses helper creators (same structure as your original code).
     */
    @DataProvider(name = "eventBatches")
    public Object[][] eventBatchProvider() {
        List<Map<String, Object>> segmentData = Arrays.asList(
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "add", "id", UUID.randomUUID().toString())),
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "update", "id", UUID.randomUUID().toString())),
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "remove", "id", UUID.randomUUID().toString())),
                new HashMap<>(Map.of("segment_name", "", "segment_id", SEGMENT_ID, "action", "remove", "id", UUID.randomUUID().toString())) // invalid for DLQ
        );

        List<Map<String, Object>> segmentUserData = Arrays.asList(
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "add", "id", UUID.randomUUID().toString(), "email", TEST_EMAIL, "mpid", TEST_MPID)),
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "delete", "id", UUID.randomUUID().toString(), "email", TEST_EMAIL, "mpid", TEST_MPID)),
                new HashMap<>(Map.of("segment_name", SEGMENT_NAME, "segment_id", SEGMENT_ID, "action", "remove", "id", UUID.randomUUID().toString(), "email", TEST_EMAIL, "mpid", TEST_MPID)) // invalid for DLQ
        );

        return new Object[][]{
                {segmentData, segmentUserData}
        };
    }

    /**
     * Single test: sends segment events and segment-user events in the same test run,
     * collects message IDs, polls Kafka and validates all messages together.
     */
    @Test(dataProvider = "eventBatches", description = "SQ-T6763 SQ-T6900 [mParticle Inbound] Verify the mParticle Inbound Service with all events", priority = 0)
    public void T6763_6900_verifyInboundWebhookWithKafkaConsumerNonBulking(List<HashMap<String, Object>> segmentData,
            List<HashMap<String, Object>> segmentUserData) throws Exception {

        // Collect all message ids to verify later
        List<String> sentMessageIds = new ArrayList<>();
        segmentData.forEach(segment -> sentMessageIds.add((String) segment.get("id")));
        segmentUserData.forEach(segmentUser -> sentMessageIds.add((String) segmentUser.get("id")));

        logger.info("Total messages to send: " + sentMessageIds.size());

        // Setup Kafka consumer
        KafkaConsumerUtility consumer = KafkaConsumerUtility.setupTestConsumer(
                dataSet.get("bootstrapServer"), KAFKA_TOPICS, ACCOUNT_ID);

        try {
            utils.longWaitInSeconds(10);

            // Send segment messages
            logger.info("Sending segment messages...");
            for (int i = 0; i < segmentData.size(); i++) {
                HashMap<String, Object> segment = segmentData.get(i);
                String segMsgUUID = (String) segment.get("id");
                String segmentName = (String) segment.get("segment_name");
                int segmentId = (int) segment.get("segment_id");
                String action = (String) segment.get("action");

                logger.info("Sending segment message ID: " + segMsgUUID + ", action: " + action);

                Response inboundSegResp = pageObj.endpoints().inboundSegmentEventGenerate(
                        dataSet.get("proxyUrl"),
                        dataSet.get("hostUrl"),
                        dataSet.get("apiKey"),
                        segmentName,
                        String.valueOf(segmentId),
                        action,
                        segMsgUUID
                        );

                logger.info("Segment " + action + " response: " + inboundSegResp.getStatusCode());

                int waitTime = (i == 0) ? 5 : 2;
                utils.longWaitInSeconds(waitTime);
            }

            // first poll to give system time to push segment messages
            consumer.pollForMessages(ACCOUNT_ID, 2000);

            // Send segment user messages
            logger.info("Sending segment user messages...");
            for (HashMap<String, Object> segmentUser : segmentUserData) {
                String userMsgUUID = (String) segmentUser.get("id");
                String segmentName = (String) segmentUser.get("segment_name");
                int segmentId = (int) segmentUser.get("segment_id");
                String action = (String) segmentUser.get("action");
                String email = (String) segmentUser.get("email");
                String mpid = (String) segmentUser.get("mpid");

                logger.info("Sending segment user message ID: " + userMsgUUID + ", action: " + action);

                Response inboundSegUserResp = pageObj.endpoints().inboundSegmentUserEventGenerate(
                        dataSet.get("proxyUrl"),
                        dataSet.get("hostUrl"),
                        dataSet.get("apiKey"),
                        segmentName,
                        String.valueOf(segmentId),
                        action,
                        userMsgUUID,
                        email,
                        true,
                        dataSet.get("hostUrl"),
                        mpid
                        );

                logger.info("Segment User " + action + " response: " + inboundSegUserResp.getStatusCode());
                utils.longWaitInSeconds(2);
            }

            // Final wait and poll
            utils.longWaitInSeconds(30);
            consumer.pollForMessages(ACCOUNT_ID, 5000);

            // ---------------- Updated getAllMessages to support List<JSONObject> ----------------
            Map<String, Map<String, List<JSONObject>>> allMessages = consumer.getAllMessages();

            for (Map.Entry<String, Map<String, List<JSONObject>>> topicEntry : allMessages.entrySet()) {
                String topic = topicEntry.getKey();
                Map<String, List<JSONObject>> messages = topicEntry.getValue();
                logger.info("Topic: " + topic + ", Message IDs: " + messages.size());

                for (Map.Entry<String, List<JSONObject>> msgEntry : messages.entrySet()) {
                    String msgId = msgEntry.getKey();
                    List<JSONObject> msgList = msgEntry.getValue();
                    logger.info("  Message ID: " + msgId + ", Total messages: " + msgList.size());

                    for (JSONObject msg : msgList) {
                        logger.info("    Payload: " + msg.toString());
                    }
                }
            }

            // Verify all messages were received
            Map<String, Boolean> messageResults = consumer.verifyMessages(sentMessageIds);
            logger.info("Message results: " + messageResults);

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

            Assert.assertEquals(found, sentMessageIds.size(),
                    "Not all messages were found. Found " + found + " out of " + sentMessageIds.size() +
                    ". " + missingMessages.toString());

            // Validate message payloads
            validateAllMessages(consumer, sentMessageIds);

        } finally {
            consumer.close();
        }
    }

    // ---------------- Validation Methods ----------------
    public void validateAllMessages(KafkaConsumerUtility consumer, List<String> sentMessageIds) {
        Map<String, JSONObject> messages = new HashMap<>();

        for (String id : sentMessageIds) {
            // Updated to use the new utility method that returns a list
            List<JSONObject> msgs = consumer.getMessagesByIdFromAnyTopic(id);
            if (!msgs.isEmpty()) {
                JSONObject msg = msgs.get(0); // pick first message for validation
                messages.put(id, msg);
                logger.info("Retrieved message with ID: " + id + ", type: " +
                        (msg.has("message_type") ? msg.getString("message_type") : "unknown"));
            } else {
                logger.error("Failed to retrieve message with ID: " + id);
                TestListeners.extentTest.get().warning("Message with ID " + id + " not found in any topic");
            }
        }

        try {
            // Validate segment messages (indices assumed by the order we added them)
            if (messages.containsKey(sentMessageIds.get(0))) {
                validateSegmentPayload(messages.get(sentMessageIds.get(0)), BIZ_NAME, "add",
                        SEGMENT_ID, SEGMENT_NAME, "segment", "mparticle");
            }
            if (messages.containsKey(sentMessageIds.get(1))) {
                validateSegmentPayload(messages.get(sentMessageIds.get(1)), BIZ_NAME, "update",
                        SEGMENT_ID, SEGMENT_NAME, "segment", "mparticle");
            }
            if (messages.containsKey(sentMessageIds.get(2))) {
                validateSegmentPayload(messages.get(sentMessageIds.get(2)), BIZ_NAME, "remove",
                        SEGMENT_ID, SEGMENT_NAME, "segment", "mparticle");
            }

            // Validate DLQ for invalid segment (4th)
            if (messages.containsKey(sentMessageIds.get(3))) {
                validateDlqPayload(messages.get(sentMessageIds.get(3)),
                        "message validation failed: audience_name field is mandatory",
                        "VALIDATION_ERROR", 400, "6071", "segment");
            }

            // Validate segment user messages (next indices)
            if (messages.containsKey(sentMessageIds.get(4))) {
                validateSegmentUserPayload(messages.get(sentMessageIds.get(4)), BIZ_NAME, "user",
                        "audience_membership_change_request", "add", SEGMENT_ID, SEGMENT_NAME,
                        TEST_MPID, "punchh_user_id", "partnerId", TEST_EMAIL);
            }
            if (messages.containsKey(sentMessageIds.get(5))) {
                validateSegmentUserPayload(messages.get(sentMessageIds.get(5)), BIZ_NAME, "user",
                        "audience_membership_change_request", "delete", SEGMENT_ID, SEGMENT_NAME,
                        TEST_MPID, "punchh_user_id", "partnerId", TEST_EMAIL);
            }

            // Validate DLQ for invalid segment user (7th)
            if (messages.containsKey(sentMessageIds.get(6))) {
                validateDlqPayload(messages.get(sentMessageIds.get(6)),
                        "message validation failed: user_profiles doesn't have valid action type",
                        "VALIDATION_ERROR", 400, "6071", "user");
            }

            int validatedCount = messages.size();
            if (validatedCount == sentMessageIds.size()) {
                TestListeners.extentTest.get().pass("All validations passed successfully for " +
                        validatedCount + " messages");
            } else {
                TestListeners.extentTest.get().warning("Validated " + validatedCount +
                        " out of " + sentMessageIds.size() + " messages");
            }
        } catch (Exception e) {
            logger.error("Error during message validation: " + e.getMessage(), e);
            Assert.fail("Message validation failed with exception: " + e.getMessage());
        }
    }

    public void validateSegmentPayload(JSONObject payload, String businessName, String action,
            int audienceId, String audienceName, String messageType, String source) {
        Assert.assertEquals(payload.getString("business_name"), businessName, "business_name mismatch");
        Assert.assertEquals(payload.getString("message_type"), messageType, "message_type mismatch");
        Assert.assertEquals(payload.getString("source"), source, "source mismatch");

        JSONObject payloadObj = payload.getJSONObject("payload");
        Assert.assertEquals(payloadObj.getString("action"), action, "action mismatch");
        Assert.assertEquals(payloadObj.getInt("audience_id"), audienceId, "audience_id mismatch");
        Assert.assertEquals(payloadObj.getString("audience_name"), audienceName, "audience_name mismatch");

        logger.info("Segment payload validation passed for ID: " + payloadObj.optString("id", "N/A"));
        TestListeners.extentTest.get().info("Segment payload validation passed for ID: " +
                payloadObj.optString("id", "N/A"));
    }

    public void validateSegmentUserPayload(JSONObject payload, String businessName, String messageType,
            String type, String action, int audienceId, String audienceName,
            String mpid, String partnerIdentityType, String partnerIdentityValue,
            String userIdentityValue) {
        Assert.assertEquals(payload.getString("business_name"), businessName, "business_name mismatch");
        Assert.assertEquals(payload.getString("message_type"), messageType, "message_type mismatch");

        JSONObject payloadObj = payload.getJSONObject("payload");
        Assert.assertEquals(payloadObj.getString("type"), type, "type mismatch");

        JSONObject userProfile = payloadObj.getJSONArray("user_profiles").getJSONObject(0);
        Assert.assertEquals(userProfile.getString("mpid"), mpid, "mpid mismatch");

        JSONObject audience = userProfile.getJSONArray("audiences").getJSONObject(0);
        Assert.assertEquals(audience.getString("action"), action, "action mismatch");
        Assert.assertEquals(audience.getInt("audience_id"), audienceId, "audience_id mismatch");
        Assert.assertEquals(audience.getString("audience_name"), audienceName, "audience_name mismatch");

        JSONObject partnerIdentity = userProfile.getJSONArray("partner_identities").getJSONObject(0);
        Assert.assertEquals(partnerIdentity.getString("type"), partnerIdentityType, "partner_identity_type mismatch");
        Assert.assertEquals(partnerIdentity.getString("value"), partnerIdentityValue, "partner_identity_value mismatch");

        JSONObject userIdentity = userProfile.getJSONArray("user_identities").getJSONObject(0);
        Assert.assertEquals(userIdentity.getString("value"), userIdentityValue, "user_identity_value mismatch");

        logger.info("Segment user payload validation passed for ID: " + payloadObj.optString("id", "N/A"));
        TestListeners.extentTest.get().info("Segment user payload validation passed for ID: " +
                payloadObj.optString("id", "N/A"));
    }

    public void validateDlqPayload(JSONObject payload, String error, String errorCodeWrapped,
            int wrappedStatusCode, String identifier, String messageType) {
        JSONObject payloadObj = payload.getJSONObject("message");

        Assert.assertEquals(payload.getString("error"), error, "error message mismatch");
        Assert.assertEquals(payload.getString("error_code_wrapped"), errorCodeWrapped, "error_code_wrapped mismatch");
        Assert.assertEquals(payload.getInt("wrapped_status_code"), wrappedStatusCode, "wrapped_status_code mismatch");
        Assert.assertEquals(payload.getString("identifier"), identifier, "identifier mismatch");
        Assert.assertEquals(payload.getString("message_type"), messageType, "message_type mismatch");

        logger.info("DLQ payload validation passed for ID: " + payloadObj.optString("id", "N/A"));
        TestListeners.extentTest.get().info("DLQ payload validation passed for ID: " +
                payloadObj.optString("id", "N/A"));
    }

    @AfterMethod
    public void afterClass() {
        dataSet.clear();
        logger.info("Test data cleared");
    }
}
