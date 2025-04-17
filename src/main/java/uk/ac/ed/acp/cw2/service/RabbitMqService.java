package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RabbitMqService {

    private ConnectionFactory factory;

    private final RuntimeEnvironment environment;
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqService.class);

    // 构造器注入
    public RabbitMqService(RuntimeEnvironment environment) {
        this.environment = environment;
        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public void sendMessage(String queueName, Map<String, Object> message) {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);
            String json = mapper.writeValueAsString(message);
            channel.basicPublish("", queueName, null, json.getBytes(StandardCharsets.UTF_8));
            logger.info(String.format("RabbitMQ send msg to queue name: %s, content: %s", queueName, json));
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ message send failed", e);  // TODO Exception
        }
    }

    // Read one message from the readQueue (rabbitMQ) with no time limit
    public String readJsonMessage(String queueName) {
        logger.info("Reading messages from queue '{}', no time limit", queueName);
        String message;

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            while (true) {
                GetResponse response = channel.basicGet(queueName, true);  // true = auto-ack
                if (response != null) {
                    message = new String(response.getBody(), StandardCharsets.UTF_8);
                    logger.debug("Received message: {}", message);
                    break;
                } else {
                    // 没消息时短暂休眠避免空转
                    Thread.sleep(50);
                }
            }
            return message;
        } catch (Exception e) {
            logger.error("Failed to read from queue '{}': {}", queueName, e.getMessage(), e);
            throw new RuntimeException("RabbitMQ message read failed", e);   // TODO Exception
        }
    }

    // Write one message to the writeQueue
    public void writeJsonMessage(String queueName, String jsonMessage) {
        logger.info("Writing 1 message in queue {}", queueName);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            channel.basicPublish("", queueName, null, jsonMessage.getBytes());

            logger.info("One record sent. Content is" + jsonMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);   // TODO Exception
        }
    }
}
