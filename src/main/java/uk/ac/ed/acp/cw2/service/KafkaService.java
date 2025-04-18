package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.*;

@Service
public class KafkaService {


    private final RuntimeEnvironment environment;
    private final static Logger logger = LoggerFactory.getLogger(KafkaService.class);

    public KafkaService(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Constructs Kafka properties required for KafkaProducer and KafkaConsumer configuration.
     *
     * @param environment the runtime environment providing dynamic configuration details
     *                     such as Kafka bootstrap servers.
     * @return a Properties object containing configuration properties for Kafka operations.
     */
    private Properties getKafkaProperties(RuntimeEnvironment environment) {
        Properties kafkaProps = new Properties();
        kafkaProps.put("bootstrap.servers", environment.getKafkaBootstrapServers());
        kafkaProps.put("acks", "all");
        kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.setProperty("enable.auto.commit", "true");

        kafkaProps.put("group.id", UUID.randomUUID().toString());  // 使用新的 group.id 强制从头读
        kafkaProps.setProperty("auto.offset.reset", "earliest");   // 从头开始读取
        kafkaProps.setProperty("enable.auto.commit", "true");

        if (environment.getKafkaSecurityProtocol() != null) {
            kafkaProps.put("security.protocol", environment.getKafkaSecurityProtocol());
        }
        if (environment.getKafkaSaslMechanism() != null) {
            kafkaProps.put("sasl.mechanism", environment.getKafkaSaslMechanism());
        }
        if (environment.getKafkaSaslJaasConfig() != null) {
            kafkaProps.put("sasl.jaas.config", environment.getKafkaSaslJaasConfig());
        }

        return kafkaProps;
    }

    public List<Map<String, Object>> readKafkaMessages(String topic, int count) {
        List<Map<String, Object>> result = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        Properties kafkaProps = getKafkaProperties(environment);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            int received = 0;

            // 这里多读的是否有影响？ 无影响 读够就可以了
            while (received < count) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    try{
                        Map<String, Object> json = mapper.readValue(record.value(), Map.class);
                        result.add(json);
                        if (++received >= count) break;
                    } catch (JsonProcessingException ex) {
                        logger.warn("Skipping malformed JSON: {}", record.value(), ex);
                        // 忽略错误数据，继续处理其他消息
                    }
                }
            }
        } catch (org.apache.kafka.common.errors.TimeoutException e) {
            logger.error("Kafka poll timeout: {}", e.getMessage(), e);
        } catch (org.apache.kafka.common.KafkaException e) {
            logger.error("Kafka error while consuming topic '{}': {}", topic, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while reading Kafka messages: {}", e.getMessage(), e);
        }
        return result;
    }
}
