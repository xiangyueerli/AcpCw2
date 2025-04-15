package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class RabbitMqService {

    private ConnectionFactory factory = null;

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
            throw new RuntimeException("RabbitMQ message send failed", e);
        }
    }
}
