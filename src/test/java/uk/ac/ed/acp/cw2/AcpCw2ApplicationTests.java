package uk.ac.ed.acp.cw2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.acp.cw2.model.ProcessMessagesRequest;
import uk.ac.ed.acp.cw2.model.TransformMessagesRequest;
import uk.ac.ed.acp.cw2.service.CacheService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class AcpCw2ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CacheService cacheService;

    @Test
    void testProcessMessages() throws Exception {
        String topic = "sendQueue";   // Kafka Topic
        String wQG = "wQG";           // RabbitMQ good queue
        String wQB = "wQB";           // RabbitMQ bad queue

        // Step 1: PUT /kafka/send/sendQueue
        log.info("Step 1 - Send 20 Kafka messages to topic '{}'", topic);
        mockMvc.perform(put("/kafka/send/" + topic))
                .andExpect(status().isOk());

        // Step 2: POST /processMessages
        log.info("Step 2 - Trigger /processMessages to process Kafka messages");
        String requestBody = """
        {
          "readTopic": "sendQueue",
          "writeQueueGood": "wQG",
          "writeQueueBad": "wQB",
          "messageCount": 20
        }
        """;

        String response = mockMvc.perform(post("/processMessages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        log.info("processMessages response: {}", response);
        assertThat(response).contains("Successfully processed 20");

        // Step 3: GET /rabbitMq/wQG/1000 and /rabbitMq/wQB/1000
        log.info("Step 3 - Check wQG and wQB queues");

        String goodResult = mockMvc.perform(get("/rabbitMq/wQG/1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String badResult = mockMvc.perform(get("/rabbitMq/wQB/1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        log.info("✅ wQG messages:\n{}", goodResult);
        log.info("❌ wQB messages:\n{}", badResult);

        // Assert queue message structure contains expected fields
        assertThat(goodResult).contains("runningTotalValue").contains("uuid").contains("key");
        assertThat(badResult).contains("key").doesNotContain("uuid");

        // Assert both contain summary message
        assertThat(goodResult).contains("\\\"key\\\":\\\"TOTAL\\\"");
        assertThat(badResult).contains("\\\"key\\\":\\\"TOTAL\\\"");
    }


    @Test
    void testTransformMsg() throws Exception {
        String inQueue = "inQueue";
        String outQueue = "writeQueue";

        // Step 1. Send test data to the RabbitMQ input queue
        log.info("📤 Step 1: Send test messages to inQueue");
        mockMvc.perform(put("/rabbitMq/send/" + inQueue))
                .andExpect(status().isOk());

        // Step 2. Call transformMessages
        log.info("🔄 Step 2: Call /transformMessages");

        // Avoid being affected by existing keys
        String key = "ABC";
        if(cacheService.retrieveFromCache(key) != null) {
            cacheService.removeKey("ABC");
        }

        TransformMessagesRequest req = new TransformMessagesRequest();
        req.setReadQueue(inQueue);
        req.setWriteQueue(outQueue);
        req.setMessageCount(9);

        String responseText = mockMvc.perform(post("/transformMessages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        log.info("✅ transformMessages result: {}", responseText);

        // Assertions designed for test samples (TransformMessageData.json)
        assertThat(responseText).contains("totalMessagesProcessed: 9");
        assertThat(responseText).contains("totalMessagesWritten: 7");
        assertThat(responseText).contains("totalRedisUpdates: 5");
        assertThat(responseText).contains("totalValueWritten: 1752.50");
        assertThat(responseText).contains("totalAdded: 52.50");

        // Step 3. Read write back data from the output queue
        log.info("📥 Step 3: Read result from RabbitMQ writeQueue");

        String getResult = mockMvc.perform(get("/rabbitMq/" + outQueue + "/1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        log.info("📦 Messages in writeQueue: \n{}", getResult);

        // assert includes keyword fields
        assertThat(getResult).contains("\"key\\\"");
        assertThat(getResult).contains("\"value\\\"");
        assertThat(getResult).contains("totalMessagesProcessed");
    }
}
