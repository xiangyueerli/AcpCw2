package uk.ac.ed.acp.cw2.controller;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController()
public class RabbitMqController {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqController.class);
    private final RuntimeEnvironment environment;
    private final String[] stockSymbols = "AAPL,MSFT,GOOG,AMZN,TSLA,JPMC,CATP,UNIL,LLOY".split(",");

    public RabbitMqController(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Constructs Kafka properties required for KafkaProducer and KafkaConsumer configuration.
     *
     * @param environment the runtime environment providing dynamic configuration details
     *                     such as Kafka bootstrap servers.
     * @return a Properties object containing configuration properties for Kafka operations.
     */
    private Properties getRabbitMqProperties(RuntimeEnvironment environment) {
        Properties rabbitMqPros = new Properties();

        // TODO:

        return rabbitMqPros;
    }

    public final String StockSymbolsConfig = "stock.symbols";

    @PostMapping("/rabbitmq/sendStockSymbols/{symbolTopic}/{symbolCount}")
    public void sendStockSymbols(@PathVariable String symbolTopic, @PathVariable int symbolCount) {
        logger.info(String.format("Writing %d symbols in topic %s", symbolCount, symbolTopic));

        // TODO
    }

    @GetMapping("/rabbitmq/receiveStockSymbols/{symbolTopic}/{consumeTimeMsec}")
    public List<AbstractMap.SimpleEntry<String, String>> receiveStockSymbols(@PathVariable String symbolTopic, @PathVariable int consumeTimeMsec) {
        logger.info(String.format("Reading stock-symbols from topic %s", symbolTopic));
        return new ArrayList<>();
    }
}
