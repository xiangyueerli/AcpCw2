package uk.ac.ed.acp.cw2.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;
import uk.ac.ed.acp.cw2.service.RabbitMqService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMqController is a REST controller that provides endpoints for sending and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/rabbitMq")
public class RabbitMqController2 {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController2.class);
    private final RuntimeEnvironment environment;

    private ConnectionFactory factory = null;

    public RabbitMqController2(RuntimeEnvironment environment) {
        this.environment = environment;

        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());
    }

    // 发送第四大题的messages
    @PutMapping("send/{queueName}")
    public ResponseEntity<String> sendConstantMsg(@PathVariable String queueName) {
        logger.info("Writing constant messages in queue {}", queueName);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            ObjectMapper mapper = new ObjectMapper(); // 更可靠的 JSON 构造方式

            // 从资源文件中 读取Json消息
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data/MessageData.json");

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

                channel.basicPublish("", queueName, null, messageJson.getBytes(StandardCharsets.UTF_8));
                logger.info("Sent message: {} to queue: {}", messageJson, queueName);
            }

            return ResponseEntity.ok("Sent " + data_len + " messages to " + queueName);
        } catch (Exception e) {
            logger.error("Error sending messages to RabbitMQ: {}", e.getMessage(), e);   // TODO exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send messages: " + e.getMessage());
        }
    }

    @PutMapping("/{queueName}/{messageCount}")
    public ResponseEntity<?> sendStudentId(@PathVariable String queueName, @PathVariable int messageCount) {
        logger.info("Writing {} id-messages in queue {}", messageCount, queueName);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);
            ObjectMapper mapper = new ObjectMapper();

            for (int i = 0; i < messageCount; i++) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("uid", "s2653520");
                messageMap.put("counter", i);

                String messageJson = mapper.writeValueAsString(messageMap);

                channel.basicPublish("", queueName, null, messageJson.getBytes(StandardCharsets.UTF_8));
                logger.info(" [x] Sent message: {} to queue: {}", messageJson, queueName);
            }

            logger.info("{} message(s) sent to RabbitMQ queue '{}'", messageCount, queueName);
            return ResponseEntity.ok("Sent " + messageCount + " messages to " + queueName);
            // 503
        } catch (IOException e) {
            logger.error("RabbitMQ connection IO error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Could not connect to RabbitMQ (I/O issue).");
            // 408
        } catch (TimeoutException e) {
            logger.error("RabbitMQ connection timed out: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("RabbitMQ connection timed out.");
            // 500
        } catch (Exception e) {
            logger.error("Unexpected error when sending to RabbitMQ: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/{queueName}/{timeoutInMsec}")
    public ResponseEntity<List<String>> receiveStudentId(@PathVariable String queueName, @PathVariable int timeoutInMsec) {
        logger.info("Reading messages from queue '{}', timeout = {}ms", queueName, timeoutInMsec);
        List<String> result = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            while (System.currentTimeMillis() - startTime < timeoutInMsec) {
                GetResponse response = channel.basicGet(queueName, true);  // true = auto-ack
                if (response != null) {
                    String message = new String(response.getBody(), StandardCharsets.UTF_8);
                    result.add(message);
                    logger.debug("Received message: {}", message);
                } else {
                    // 这个空转的含义是什么？ -- 如果没收到 就等10millis 再重新试着Get  否则会一直反复调用Get
                    // 没消息时短暂休眠避免空转
                    Thread.sleep(10);  // 防止busy waiting
                }
            }

            // 确保总耗时不超过 timeout + 200ms
            long totalElapsed = System.currentTimeMillis() - startTime;
            if (totalElapsed > timeoutInMsec + 200) {
                logger.warn("Timeout exceeded: total elapsed = {}ms", totalElapsed);
            } else {
                logger.info("Finished within expected time: {}ms", totalElapsed);
            }

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("RabbitMQ I/O error while reading queue '{}': {}", queueName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
        } catch (TimeoutException e) {
            logger.error("RabbitMQ connection timeout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(Collections.emptyList());
        } catch (InterruptedException e) {
            logger.warn("Interrupted while sleeping between polls: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();  // 恢复中断状态
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Unexpected error when reading from RabbitMQ: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}
