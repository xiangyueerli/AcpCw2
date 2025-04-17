package uk.ac.ed.acp.cw2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

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
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(acpStorageServiceUrl + "/api/v1/blob"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(json)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().replace("\"", "");
        } catch (Exception e) {
            throw new RuntimeException("ACP Storage failed", e);
        }
    }
}
