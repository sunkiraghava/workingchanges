package com.punchh.server.utilities;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kafka consumer utility to consume messages from topics, store them by message ID,
 * and allow retrieval of messages either by topic or from any topic.
 */
public class KafkaConsumerUtility {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerUtility.class.getName());
    private static final Duration POLL_DURATION = Duration.ofMillis(100);
    private static final Duration INIT_DELAY = Duration.ofSeconds(1);
    private static final Duration STARTUP_DELAY = Duration.ofSeconds(2);

    private static volatile KafkaConsumerUtility instance;
    private static final Object INSTANCE_LOCK = new Object();

    private final KafkaConsumer<String, String> consumer;
    private final Map<String, Map<String, List<JSONObject>>> topicMessageStore;
    private final ReentrantLock consumerLock = new ReentrantLock();
    private Thread backgroundThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private KafkaConsumerUtility(String bootstrapServers) {
        this.consumer = createConsumer(bootstrapServers);
        this.topicMessageStore = new ConcurrentHashMap<>();
    }

    private KafkaConsumer<String, String> createConsumer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        return new KafkaConsumer<>(props);
    }

    public static KafkaConsumerUtility getInstance(String bootstrapServers) {
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new KafkaConsumerUtility(bootstrapServers);
                }
            }
        }
        return instance;
    }

    public static KafkaConsumerUtility resetInstance(String bootstrapServers) {
        synchronized (INSTANCE_LOCK) {
            if (instance != null) {
                instance.close();
            }
            instance = new KafkaConsumerUtility(bootstrapServers);
            return instance;
        }
    }

    public void startConsuming(List<String> topics) {
        clearMessages();
        topics.forEach(topic -> topicMessageStore.put(topic, new ConcurrentHashMap<>()));

        consumerLock.lock();
        try {
            consumer.subscribe(topics);
            consumer.poll(Duration.ofMillis(0));
        } finally {
            consumerLock.unlock();
        }

        try {
            Thread.sleep(INIT_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void startBackgroundConsumption(int businessId) {
        if (backgroundThread != null && backgroundThread.isAlive()) {
            return;
        }

        running.set(true);
        backgroundThread = new Thread(() -> runBackgroundLoop(businessId));
        backgroundThread.setName("kafka-background-consumer");
        backgroundThread.setDaemon(true);
        backgroundThread.start();

        try {
            Thread.sleep(STARTUP_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runBackgroundLoop(int businessId) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<String, String> records = pollSafely();
                processRecords(records, businessId);

                if (records.isEmpty()) {
                    Thread.sleep(50);
                }
            } catch (InterruptException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Background consumer error: {0}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private ConsumerRecords<String, String> pollSafely() {
        consumerLock.lock();
        try {
            return consumer.poll(POLL_DURATION);
        } finally {
            consumerLock.unlock();
        }
    }

    public void stopBackgroundConsumption() {
        running.set(false);
        if (backgroundThread != null) {
            backgroundThread.interrupt();
            try {
                backgroundThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            backgroundThread = null;
        }
    }

    private void processRecords(ConsumerRecords<String, String> records, int businessId) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                String value = record.value();
                if (value == null || value.trim().isEmpty()) continue;

                JSONObject message = new JSONObject(value);
                int messageBusinessId = extractBusinessId(message, record.topic());

                if (businessId != 0 && messageBusinessId != businessId) continue;

                String messageId = extractMessageId(message, record.topic());
                if (messageId != null) {
                    storeMessage(record.topic(), messageId, message);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing message: {0}", e.getMessage());
            }
        }
    }

    private void storeMessage(String topic, String messageId, JSONObject message) {
        Map<String, List<JSONObject>> topicMessages = topicMessageStore.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        topicMessages.computeIfAbsent(messageId, k -> new ArrayList<>()).add(message);
    }

    public Map<String, Map<String, List<JSONObject>>> pollForMessages(int businessId, long consumerTimeMs) {
        boolean wasBackgroundRunning = running.get();
        if (wasBackgroundRunning) {
            stopBackgroundConsumption();
        }

        try {
            long endTime = System.currentTimeMillis() + consumerTimeMs;
            while (System.currentTimeMillis() < endTime) {
                ConsumerRecords<String, String> records = pollSafely();
                processRecords(records, businessId);

                if (records.isEmpty()) {
                    Thread.sleep(50);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error polling messages: {0}", e.getMessage());
        } finally {
            if (wasBackgroundRunning) {
                startBackgroundConsumption(businessId);
            }
        }

        return getAllMessages();
    }

    private int extractBusinessId(JSONObject message, String topic) {
        try {
            if (topic.contains("global_inbound_dlq") || topic.contains("inbound_dlq")) {
                return extractFromPath(message, "message.account.account_id");
            } else {
                return extractFromPath(message, "payload.account.account_id");
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractMessageId(JSONObject message, String topic) {
        try {
            if (topic.contains("global_inbound_dlq") || topic.contains("inbound_dlq")) {
                return extractFromPath(message, "message.id");
            } else {
                return extractFromPath(message, "payload.id");
            }
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T extractFromPath(JSONObject message, String path) {
        String[] keys = path.split("\\.");
        Object current = message;

        for (String key : keys) {
            if (current instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) current;
                if (jsonObj.has(key) && !jsonObj.isNull(key)) {
                    current = jsonObj.get(key);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        T result = (T) current;
        return result;
    }

    // --------------- Public Retrieval Methods ---------------

    public List<JSONObject> getMessagesById(String topic, String messageId) {
        Map<String, List<JSONObject>> topicMessages = topicMessageStore.get(topic);
        return topicMessages != null ? new ArrayList<>(topicMessages.getOrDefault(messageId, List.of())) : List.of();
    }

    public List<JSONObject> getMessagesByIdFromAnyTopic(String messageId) {
        for (Map<String, List<JSONObject>> topicMessages : topicMessageStore.values()) {
            if (topicMessages.containsKey(messageId)) {
                return new ArrayList<>(topicMessages.get(messageId));
            }
        }
        return List.of();
    }

    public Map<String, List<JSONObject>> getMessagesForTopic(String topic) {
        Map<String, List<JSONObject>> messages = topicMessageStore.get(topic);
        return messages != null ? new HashMap<>(messages) : new HashMap<>();
    }

    public Map<String, Map<String, List<JSONObject>>> getAllMessages() {
        Map<String, Map<String, List<JSONObject>>> result = new HashMap<>();
        topicMessageStore.forEach((topic, messages) ->
                result.put(topic, new HashMap<>(messages)));
        return result;
    }

    public void clearMessages() {
        topicMessageStore.values().forEach(Map::clear);
        topicMessageStore.clear();
    }

    public Map<String, Boolean> verifyMessages(List<String> messageIds) {
        Map<String, Boolean> results = new HashMap<>();
        messageIds.forEach(messageId ->
                results.put(messageId, !getMessagesByIdFromAnyTopic(messageId).isEmpty()));
        return results;
    }

    public void close() {
        stopBackgroundConsumption();

        consumerLock.lock();
        try {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (InterruptException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            consumerLock.unlock();
        }

        clearMessages();
        synchronized (INSTANCE_LOCK) {
            instance = null;
        }
    }

    public static KafkaConsumerUtility setupTestConsumer(String bootstrapServers,
                                                          List<String> topics, int businessId) {
        KafkaConsumerUtility consumer = resetInstance(bootstrapServers);
        consumer.startConsuming(topics);
        consumer.startBackgroundConsumption(businessId);
        return consumer;
    }
}
