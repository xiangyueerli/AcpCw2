package uk.ac.ed.acp.cw2.controller;


import com.rabbitmq.client.DeliverCallback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RabbitMqController is a REST controller that provides endpoints for sending and receiving stock symbols
 * through RabbitMQ. This class interacts with a RabbitMQ environment which is configured dynamically during runtime.
 */
@RestController()
@RequestMapping("/api/v1/rabbitmq")
public class RabbitMqController {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
    private final RuntimeEnvironment environment;
    private final String[] stockSymbols = "AAPL,MSFT,GOOG,AMZN,TSLA,JPMC,CATP,UNIL,LLOY".split(",");

    private ConnectionFactory factory = null;

    public RabbitMqController(RuntimeEnvironment environment) {
        this.environment = environment;
        factory = new ConnectionFactory();
        factory.setHost(environment.getRabbitMqHost());
        factory.setPort(environment.getRabbitMqPort());
    }


    public final String StockSymbolsConfig = "stock.symbols";

    @PostMapping("/sendStockSymbols/{queueName}/{symbolCount}")
    public void sendStockSymbols(@PathVariable String queueName, @PathVariable int symbolCount) {
        logger.info("Writing {} symbols in queue {}", symbolCount, queueName);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(queueName, false, false, false, null);

            for (int i = 0; i < symbolCount; i++) {
                final String symbol = stockSymbols[new Random().nextInt(stockSymbols.length)];
                final String value = String.valueOf(i);

                String message = String.format("%s:%s", symbol, value);

                channel.basicPublish("", queueName, null, message.getBytes());
                System.out.println(" [x] Sent message: " + message + " to queue: " + queueName);
            }

            logger.info("{} record(s) sent to Kafka\n", symbolCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/receiveStockSymbols/{queueName}/{consumeTimeMsec}")
    public List<String> receiveStockSymbols(@PathVariable String queueName, @PathVariable int consumeTimeMsec) {
        logger.info(String.format("Reading stock-symbols from queue %s", queueName));
        List<String> result = new ArrayList<>();

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {


            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[%s]:%s -> %s", queueName, delivery.getEnvelope().getRoutingKey(), message);
                result.add(message);
            };

            System.out.println("start consuming events - to stop press CTRL+C");
            // Consume with Auto-ACK
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
            Thread.sleep(consumeTimeMsec);   // TODO 不加休眠会怎么样 猜测休眠时间一直在获取消息

            System.out.printf("done consuming events. %d record(s) received\n", result.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
