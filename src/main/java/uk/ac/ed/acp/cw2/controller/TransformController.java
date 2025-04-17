package uk.ac.ed.acp.cw2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.acp.cw2.model.NormalMessage;
import uk.ac.ed.acp.cw2.model.TotalMessage;
import uk.ac.ed.acp.cw2.model.TransformMessagesRequest;
import uk.ac.ed.acp.cw2.service.CacheService;
import uk.ac.ed.acp.cw2.service.RabbitMqService;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
public class TransformController {
    private static final Logger logger = LoggerFactory.getLogger(TransformController.class);   // 为什么这里用static  原因：Logger 通常是与类绑定的，不依赖实例字段，也不需要在不同实例间变化，所以用 static final。
    private final CacheService cacheService;
    private final RabbitMqService rabbitMqService;

    public TransformController(CacheService cacheService, RabbitMqService rabbitMqService) {
        this.cacheService = cacheService;
        this.rabbitMqService = rabbitMqService;
    }

    //
    @PostMapping("/transformMessages")
    public ResponseEntity<String> transformMessages(@RequestBody TransformMessagesRequest transformMessagesRequest) throws JsonProcessingException, InterruptedException {
        // {key : "{key:__, version:__, value:__}"}

        // 从请求体中获取参数
        String readQueue = transformMessagesRequest.getReadQueue();
        String writeQueue = transformMessagesRequest.getWriteQueue();
        int messageCount = transformMessagesRequest.getMessageCount();

        // 定义统计变量
        int totalMessagesWritten = 0;
        int totalMessagesProcessed = 0;
        int totalRedisUpdates = 0;
        float totalValueWritten = 0.0f;
        float totalAdded = 0.0f;

        // Jackson 用于 JSON 解析与生成
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < messageCount; i++) {
            // 1. 读取一条消息
            String msgString = rabbitMqService.readJsonMessage(readQueue);

            while (msgString == null) {
                // 如果队列里没有消息可读，可能返回 null
                // 视需求决定 break 或者继续等待
                msgString = rabbitMqService.readJsonMessage(readQueue);
                logger.warn("No more messages in queue {}", readQueue);
                Thread.sleep(50);
            }

            // 2. 解析 JSON，先尝试当 normal message
            // 由于题目保证消息格式正确，要区分 tombstone 的方法
            NormalMessage msg = objectMapper.readValue(msgString, NormalMessage.class);
            boolean isTombstone = isTombstone(msg);

            if(isTombstone) {
                //  3. tombstone：从消息中至少要有 key
                //    这里假设你先解析到 msg.key
                //    你也可以再写个 TombstoneMessage 类更精确
                String key = msg.getKey();
                if (key == null) {
                    key = "unknown"; // 或 throw new ...
                } // TODO

                cacheService.removeKey(msg.getKey());

                totalMessagesProcessed++;

                // 构造并写入 summary 消息
                TotalMessage totalMessage = new TotalMessage(
                        totalMessagesWritten,
                        totalMessagesProcessed,
                        totalRedisUpdates,
                        totalValueWritten,
                        totalAdded
                );
                String totalMessageString =  objectMapper.writeValueAsString(totalMessage);
                rabbitMqService.writeJsonMessage(writeQueue, totalMessageString);

//                totalMessagesWritten++; // 写了 1 条 summary  不需要 special永远不会录入totalMessagesWritten

            }
            else {
                // normal message
                // 判断 redis
                String key = msg.getKey();
                Integer version = msg.getVersion(); // 1..n
                Float value = msg.getValue();

                // 拉取当前 redis 中的 record
                String cacheValueStr = cacheService.retrieveFromCache(key);

                if (cacheValueStr == null) {
                    cacheService.storeInCache(key, msgString);

                    totalRedisUpdates++;

                    // send to rabbitmq
                    value = value + 10.5f;
                    msg.setValue(value);
                    String msgStringNew = objectMapper.writeValueAsString(msg);
                    logger.info("msg: {}, msgStringNew: {}.", msg, msgStringNew);
                    rabbitMqService.writeJsonMessage(writeQueue, msgStringNew);

                    totalAdded += 10.5f;
                    totalValueWritten += value;

                    totalMessagesWritten++;
                    totalMessagesProcessed++;
                }
                else {
                    NormalMessage cachedMsg = objectMapper.readValue(cacheValueStr, NormalMessage.class);
                    if (cachedMsg.getVersion() < version) {
                        cacheService.storeInCache(key, msgString);

                        totalRedisUpdates++;

                        // send to rabbitmq
                        value = value + 10.5f;
                        msg.setValue(value);
                        String msgStringNew = objectMapper.writeValueAsString(msg);
                        rabbitMqService.writeJsonMessage(writeQueue, msgStringNew);

                        totalAdded += 10.5f;
                        totalValueWritten += value;

                        totalMessagesWritten++;
                        totalMessagesProcessed++;
                    }
                    // TODO
                    else {
                        // redis 中 version == 新消息 => 不更新 redis
                        // 原样写入
                        rabbitMqService.writeJsonMessage(writeQueue, msgString);   //
                        totalValueWritten += value; // 记录这次写出的 value
                        totalMessagesWritten++;
                        totalMessagesProcessed++;
                    }
                }
            }
        }
        // 最终返回处理结果
        String resultMsg = String.format(
                "totalMessagesWritten: %d, totalMessagesProcessed: %d, totalRedisUpdates: %d, totalValueWritten: %.2f, totalAdded: %.2f",
                totalMessagesWritten, totalMessagesProcessed, totalRedisUpdates, totalValueWritten, totalAdded
        );
        return ResponseEntity.ok(resultMsg);
    }

    public boolean isTombstone(NormalMessage msg) {
        return msg.getVersion() == null ||  msg.getValue() == null;
    }
}
