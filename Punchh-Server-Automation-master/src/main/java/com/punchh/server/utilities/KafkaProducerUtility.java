package com.punchh.server.utilities;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KafkaProducerUtility {

    private static KafkaProducerUtility instance;
    private final KafkaProducer<String, String> producer;

    // Private constructor for singleton
    private KafkaProducerUtility(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    /**
     * Get the singleton instance of KafkaProducerUtility.
     *
     * @param bootstrapServers Kafka bootstrap servers.
     * @return Singleton instance of KafkaProducerUtility.
     */
    public static synchronized KafkaProducerUtility getInstance(String bootstrapServers) {
        if (instance == null) {
            instance = new KafkaProducerUtility(bootstrapServers);
        }
        return instance;
    }

    /**
     * Reads the content of a JSON file as a String.
     *
     * @param filePath Path to the JSON file.
     * @return Content of the JSON file as a String.
     */
    public static String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.printf("Error reading JSON file at %s: %s%n", filePath, e.getMessage());
            return null; // Return null if reading fails
        }
    }

    /**
     * Sends messages to specified Kafka topics.
     *
     * @param topicMessages A map where keys are topic names and values are lists of messages to send.
     */
    public void sendMessages(Map<String, List<String>> topicMessages) {
        topicMessages.forEach((topic, messages) -> {
            for (String message : messages) {
                try {
                    ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
                    producer.send(record, (metadata, exception) -> {
                        if (exception == null) {
                            System.out.printf("Message '%s' sent to topic '%s' (partition %d, offset %d)%n",
                                    message, topic, metadata.partition(), metadata.offset());
                        } else {
                            System.err.printf("Error sending message to topic '%s': %s%n", topic, exception.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.printf("Failed to send message to topic '%s': %s%n", topic, e.getMessage());
                }
            }
        });
    }

    /**
     * Closes the Kafka producer.
     */
    public void close() {
        try {
            producer.close();
        } catch (Exception e) {
            System.err.printf("Error closing Kafka producer: %s%n", e.getMessage());
        }
    }
    
    public void sendMessageOnKafkaTopics(String devBootstrapServers, Map<String, List<String>> topicMessages) {
		// Get the singleton instance of KafkaProducerUtility
    	getInstance(devBootstrapServers);
		// Send messages to Kafka topics
		sendMessages(topicMessages);
	}
}
