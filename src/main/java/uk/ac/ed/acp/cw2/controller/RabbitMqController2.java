package uk.ac.ed.acp.cw2.controller;


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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * RabbitMqController is a REST controller that provides endpoints for sending and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/rabbitMq")
public class RabbitMqController2 {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController2.class);
    private final RuntimeEnvironment environment;

    private RabbitMqService rabbitMqService;

    private ConnectionFactory factory = null;

    public RabbitMqController2(RuntimeEnvironment environment, RabbitMqService rabbitMqService) {
        this.environment = environment;
        this.rabbitMqService = rabbitMqService;

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
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("data/messageData.json");

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
    public ResponseEntity<String> sendStudentId(@PathVariable String queueName, @PathVariable int messageCount) {
        logger.info("Writing {} id-messages in queue {}", messageCount, queueName);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            ObjectMapper mapper = new ObjectMapper(); // 更可靠的 JSON 构造方式

            for (int i = 0; i < messageCount; i++) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("uid", "s2653520");  // TODO: 可以从配置读取
                messageMap.put("counter", i);

                String messageJson = mapper.writeValueAsString(messageMap);

                channel.basicPublish("", queueName, null, messageJson.getBytes(StandardCharsets.UTF_8));
                logger.info(" [x] Sent message: {} to queue: {}", messageJson, queueName);
            }

            logger.info("{} message(s) sent to RabbitMQ queue '{}'", messageCount, queueName);
            return ResponseEntity.ok("Sent " + messageCount + " messages to " + queueName);
        } catch (Exception e) {
            logger.error("Error sending messages to RabbitMQ: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send messages: " + e.getMessage());
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
                    Thread.sleep(10);
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
        } catch (Exception e) {
            logger.error("Failed to read from queue '{}': {}", queueName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
}
