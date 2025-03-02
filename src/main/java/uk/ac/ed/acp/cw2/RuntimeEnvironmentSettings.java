package uk.ac.ed.acp.cw2;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuntimeEnvironmentSettings {
    private String redisHost;
    private int redisPort;
    private String rabbitmqHost;
    private int rabbitmqPort;
    private String kafkaBootstrapServers;
}
