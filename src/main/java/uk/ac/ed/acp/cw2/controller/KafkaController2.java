package uk.ac.ed.acp.cw2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            ObjectMapper mapper = new ObjectMapper(); // 更可靠的 JSON 构造方式
            for (int i = 0; i < messageCount; i++) {

                // 1. 构建要发送的消息内容（Map → JSON）
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("uid", "s2653520");  // TODO: 可以从配置读取
                messageMap.put("counter", i);

                String messageJson = mapper.writeValueAsString(messageMap);

                // 2. 创建 ProducerRecord
                //    - key 可以是 null 或自定义字符串
                String key = "k" + i;
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(writeTopic, key, messageJson);

                // 3. 使用回调，及时了解发送是否成功
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

        } catch (ExecutionException | TimeoutException | InterruptedException | JsonProcessingException e) {
            // 捕获各种异常并返回 500
            logger.error("Error producing to Kafka topic '{}': {}", writeTopic, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send messages: " + e.getMessage());
        }
    }

    // 发送第三大题的request
    @PutMapping("send/{writeTopic}/{messageCount}")
    public ResponseEntity<String> sendStudentId2(@PathVariable String writeTopic, @PathVariable int messageCount) {
        logger.info(String.format("Writing %d messages in topic %s", messageCount, writeTopic));
        Properties kafkaProps = getKafkaProperties(environment);

        try (var producer = new KafkaProducer<String, String>(kafkaProps)) {
            ObjectMapper mapper = new ObjectMapper(); // 更可靠的 JSON 构造方式
            for (int i = 0; i < messageCount; i++) {

                // 1. 构建要发送的消息内容（Map → JSON）]
                Map<String, Object> messageMap = new HashMap<>();
                if (i % 2 == 0) {
                    messageMap.put("uid", "s2653520");  // TODO: 可以从配置读取
                    messageMap.put("key", "234");
                    messageMap.put("comment", "hahaha");
                    messageMap.put("value", 1.3);
                }
                else {
                    messageMap.put("uid", "s2653520");  // TODO: 可以从配置读取
                    messageMap.put("key", "23455");
                    messageMap.put("comment", "heiheihei");
                    messageMap.put("value", 9.99);
                }


                String messageJson = mapper.writeValueAsString(messageMap);

                // 2. 创建 ProducerRecord
                //    - key 可以是 null 或自定义字符串
                String key = "k" + i;
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(writeTopic, key, messageJson);

                // 3. 使用回调，及时了解发送是否成功
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

        } catch (ExecutionException | TimeoutException | InterruptedException | JsonProcessingException e) {
            // 捕获各种异常并返回 500
            logger.error("Error producing to Kafka topic '{}': {}", writeTopic, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send messages: " + e.getMessage());
        }
    }

    @GetMapping("/{readTopic}/{timeoutInMsec}")
    public ResponseEntity<List<String>> receiveStudentId(@PathVariable String readTopic, @PathVariable int timeoutInMsec) {
        logger.info("Reading messages from topic '{}' with a poll timeout of {} ms",
                readTopic, timeoutInMsec);
        Properties kafkaProps = getKafkaProperties(environment);

        List<String> result = new ArrayList<>();

        try (var consumer = new KafkaConsumer<String, String>(kafkaProps)) {
            consumer.subscribe(Collections.singletonList(readTopic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutInMsec));
            for (ConsumerRecord<String, String> record : records) {
                logger.info("Topic={}, Partition={}, Offset={}, Timestamp={}, Key={}, Value={}",
                        record.topic(), record.partition(), record.offset(), record.timestamp(),
                        record.key(), record.value());
                System.out.println(record.value());

                // 题目要求：每条消息在返回的 List 中占一个 String 项
                // 可以只放 record.value() 或者把 key,value 合成一个字符串都行
                result.add(record.value());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // 避免直接抛出异常导致 500，手动捕获并返回错误信息
            logger.error("Error reading from Kafka topic '{}': {}", readTopic, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}
