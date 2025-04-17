package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.acp.cw2.model.ProcessMessagesRequest;
import uk.ac.ed.acp.cw2.service.AcpStorageService;
import uk.ac.ed.acp.cw2.service.KafkaService;
import uk.ac.ed.acp.cw2.service.RabbitMqService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProcessController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);

    private final KafkaService kafkaService;
    private final AcpStorageService acpStorageService;
    private final RabbitMqService rabbitMqService;

    public ProcessController(KafkaService kafkaService, AcpStorageService acpStorageService, RabbitMqService rabbitMqService) {
        this.kafkaService = kafkaService;
        this.acpStorageService = acpStorageService;
        this.rabbitMqService = rabbitMqService;
    }

    @PostMapping("/processMessages")
    public ResponseEntity<String> processMessages(@RequestBody ProcessMessagesRequest request) {
        try {
            List<Map<String, Object>> messages = kafkaService.readKafkaMessages(
                    request.getReadTopic(),
                    request.getMessageCount()
            );

            double runningTotal = 0.0;
            double badTotal = 0.0;
            String uid =  null;

            for (Map<String, Object> msg : messages) {
                logger.info("Processing message: {}", msg);
                String key = (String) msg.get("key");
                double value = ((Number) msg.get("value")).doubleValue();   //捕捉错误 格式？


                if (key.length() == 3 || key.length() == 4) {
                    runningTotal += value;
                    msg.put("runningTotalValue", runningTotal);
                    logger.info("runningTotalValue: {}", runningTotal);
                    String uuid = acpStorageService.storeJsonToAcp(msg);
                    msg.put("uuid", uuid);
                    logger.info("uuid: {}", uuid);
                    rabbitMqService.sendMessage(request.getWriteQueueGood(), msg);
                } else if (key.length() == 5) {
                    badTotal += value;
                    rabbitMqService.sendMessage(request.getWriteQueueBad(), msg);
                }
                uid = (String) msg.get("uid");
            }

            logger.info("uuid: {}", runningTotal);
            // 构造并发送 TOTAL 消息（不需要 uuid 和存储）
            Map<String, Object> totalGood = buildTotalMessage(uid, runningTotal);
            Map<String, Object> totalBad = buildTotalMessage(uid, badTotal);
            rabbitMqService.sendMessage(request.getWriteQueueGood(), totalGood);
            rabbitMqService.sendMessage(request.getWriteQueueBad(), totalBad);

            return ResponseEntity.ok("Successfully processed " + messages.size() + " messages");
        } catch (Exception e) {
            e.printStackTrace();    // TODO 如何优化exception
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private Map<String, Object> buildTotalMessage(String uid, double value) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("uid", uid);
        msg.put("key", "TOTAL");
        msg.put("comment", "");
        msg.put("value", value);
        return msg;
    }
}