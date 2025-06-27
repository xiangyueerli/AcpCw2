package uk.ac.ed.acp.cw2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;
import uk.ac.ed.acp.cw2.service.KafkaService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * KafkaController is a REST API controller used to interact with Apache Kafka for producing
 * and consuming stock symbol events. This class provides endpoints for sending stock symbols
 * to a Kafka topic and retrieving stock symbols from a Kafka topic.
 * <p>
 * It is designed to handle dynamic Kafka configurations based on the runtime environment
 * and supports security configurations such as SASL and JAAS.
 */
@RestController()
@RequestMapping("/kafka")
public class KafkaController2 {

    private static final Logger logger = LoggerFactory.getLogger(KafkaController2.class);
    private final RuntimeEnvironment environment;
//    private final String[] stockSymbols = "AAPL,MSFT,GOOG,AMZN,TSLA,JPMC,CATP,UNIL,LLOY".split(",");

    public KafkaController2(RuntimeEnvironment environment) {
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

        kafkaProps.put("group.id", UUID.randomUUID().toString());
        kafkaProps.setProperty("auto.offset.reset", "earliest");
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

    @PutMapping("/{writeTopic}/{messageCount}")
    public ResponseEntity<String> sendStudentId(@PathVariable String writeTopic, @PathVariable int messageCount) {
        logger.info(String.format("Writing %d messages in topic %s", messageCount, writeTopic));
        Properties kafkaProps = getKafkaProperties(environment);

        try (var producer = new KafkaProducer<String, String>(kafkaProps)) {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < messageCount; i++) {

                // 1. Build the message content to be sent (Map → JSON)
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("uid", "s2653520");
                messageMap.put("counter", i);

                String messageJson = mapper.writeValueAsString(messageMap);

                // 2. Create ProducerRecord
                // - key can be null or custom string
                String key = "k" + i;
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(writeTopic, key, messageJson);

                // 3. Use callbacks to promptly understand whether the sending is successful
                producer.send(record, (recordMetadata, ex) -> {
                    if (ex != null)
                        logger.error("Error producing to Kafka", ex);
                    else {
                        logger.info("Produced event to topic={}, partition={}, offset={}, key={}, value={}",
                                recordMetadata.topic(),
                                recordMetadata.partition(),
                                recordMetadata.offset(),
                                key,
                                messageJson);
                    }
                }).get(1000, TimeUnit.MILLISECONDS);
            }
            // 发送完毕
            logger.info("{} record(s) sent to Kafka topic '{}'", messageCount, writeTopic);
            // 返回 200 OK
            return ResponseEntity.ok("Successfully produced " + messageCount + " messages to topic " + writeTopic);

        } catch (TimeoutException e) {
            logger.error("Kafka send timeout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Kafka send timeout.");
        } catch (ExecutionException e) {
            logger.error("Kafka execution error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Kafka execution error.");
        } catch (InterruptedException e) {
            logger.warn("Kafka send interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kafka send interrupted.");
        } catch (JsonProcessingException e) {
            logger.error("JSON processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to serialize message.");
        } catch (Exception e) {
            logger.error("Unexpected Kafka error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }

    // Send the messages for the ProcessMessages. Data from ProcessMessagesData.json
    @PutMapping("send/{writeTopic}")
    public ResponseEntity<String> sendStudentId2(@PathVariable String writeTopic) {
        logger.info(String.format("Writing messages in topic %s", writeTopic));
        Properties kafkaProps = getKafkaProperties(environment);

        try (var producer = new KafkaProducer<String, String>(kafkaProps)) {
            ObjectMapper mapper = new ObjectMapper(); // 更可靠的 JSON 构造方式

            // 从资源文件中 读取Json消息
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data/ProcessMessagesData.json");

            if (inputStream == null) {
                logger.info("Input stream not found.");
                return ResponseEntity.notFound().build();
            }

            List<Map<String, Object>> data = mapper.readValue(
                    inputStream,
                    new TypeReference<>() {
                    }
            );
            logger.info("data: {}", data.toString());
            int data_len = data.size();

            for (int i = 0; i < data_len; i++) {

                String messageJson = mapper.writeValueAsString(data.get(i));

                // 2. Create ProducerRecord
                // - key can be null or custom string
                String key = "k" + i;
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(writeTopic, key, messageJson);

                // 3. Use callbacks to see if the sending is successful
                producer.send(record, (recordMetadata, ex) -> {
                    if (ex != null)
                        logger.error("Error producing to Kafka", ex);
                    else {
                        logger.info("Produced event to topic={}, partition={}, offset={}, key={}, value={}",
                                recordMetadata.topic(),
                                recordMetadata.partition(),
                                recordMetadata.offset(),
                                key,
                                messageJson);
                    }
                }).get(1000, TimeUnit.MILLISECONDS);
            }
            logger.info("{} record(s) sent to Kafka topic '{}'", data_len, writeTopic);
            return ResponseEntity.ok("Successfully produced " + data_len + " messages to topic " + writeTopic);

        } catch (ExecutionException | TimeoutException | InterruptedException | JsonProcessingException e) {
            // Catch various exceptions and return 500；
            logger.error("Error producing to Kafka topic '{}': {}", writeTopic, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send messages: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{readTopic}/{timeoutInMsec}")
    public ResponseEntity<List<String>> receiveStudentId(@PathVariable String readTopic,
                                                         @PathVariable int timeoutInMsec) {
        logger.info("Reading messages from topic '{}' with a poll timeout of {} ms",
                readTopic, timeoutInMsec);
        Properties kafkaProps = getKafkaProperties(environment);

        List<String> result = new ArrayList<>();

        // 计算时间窗口
        long startTime = System.currentTimeMillis();
        long deadline = startTime + timeoutInMsec;
        long absoluteDeadline = deadline + 200; // 加 200ms 的软限制

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps)) {
            consumer.subscribe(Collections.singletonList(readTopic));

            while (System.currentTimeMillis() < deadline) {
                long remainingTime = deadline - System.currentTimeMillis();
                if (remainingTime <= 0) break;

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Math.min(remainingTime, 150)));
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("Topic={}, Partition={}, Offset={}, Timestamp={}, Key={}, Value={}",
                            record.topic(), record.partition(), record.offset(), record.timestamp(),
                            record.key(), record.value());
                    result.add(record.value());
                }
            }

            long totalElapsed = System.currentTimeMillis() - startTime;
            logger.info("Kafka polling finished in {} ms. Total records read: {}", totalElapsed, result.size());

            // 如果超时超出 +200ms，按题目要求算失败（可选）
            if (System.currentTimeMillis() > absoluteDeadline) {
                logger.warn("Exceeded maximum allowed processing time ({}ms)", timeoutInMsec + 200);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(Collections.emptyList());
            }

            return ResponseEntity.ok(result);

        } catch (org.apache.kafka.common.errors.TimeoutException e) {
            logger.error("Kafka poll timeout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(Collections.emptyList());

        } catch (org.apache.kafka.common.errors.WakeupException e) {
            logger.error("Kafka consumer interrupted: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());

        } catch (org.apache.kafka.common.KafkaException e) {
            logger.error("Kafka error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());

        } catch (Exception e) {
            logger.error("Unexpected Kafka read error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

}
