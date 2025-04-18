package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class AcpStorageService {

    private final RuntimeEnvironment environment;
    private final String acpStorageServiceUrl;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(AcpStorageService.class);

    public AcpStorageService(RuntimeEnvironment environment) {
        this.environment = environment;
        acpStorageServiceUrl = environment.getAcpStorageService();
    }

    public String storeJsonToAcp(Map<String, Object> json) {
        logger.info("AcpStorageServiceUrl: {}", acpStorageServiceUrl);
        String endpoint = acpStorageServiceUrl.endsWith("/") ?
                acpStorageServiceUrl + "api/v1/blob" :
                acpStorageServiceUrl + "/api/v1/blob";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(json)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().replace("\"", "");
        } catch (IOException | InterruptedException e) {
            logger.error("ACP Storage request error: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();  // 恢复线程状态（如果被中断）
        } catch (Exception e) {
            logger.error("Unexpected error while storing to ACP Storage: {}", e.getMessage(), e);
        }
        return null; // 明确失败时返回 null
    }
}
