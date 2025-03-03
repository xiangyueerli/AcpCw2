package uk.ac.ed.acp.cw2.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents configuration settings for runtime environments, which are populated using environment variables.
 * This class includes settings related to Redis, RabbitMQ, and Kafka. It provides static constants for
 * environment variable names and methods to retrieve and validate the required environment settings.
 */
@Getter
@Setter
public class RuntimeEnvironment {

    public static final String REDIS_HOST_ENV_VAR = "REDIS_HOST";
    public static final String REDIS_PORT_ENV_VAR = "REDIS_PORT";
    public static final String RABBITMQ_HOST_ENV_VAR = "RABBITMQ_HOST";
    public static final String RABBITMQ_PORT_ENV_VAR = "RABBITMQ_PORT";
    public static final String KAFKA_BOOTSTRAP_SERVERS_ENV_VAR = "KAFKA_BOOTSTRAP_SERVERS";

    private String redisHost;
    private int redisPort;
    private String rabbitmqHost;
    private int rabbitmqPort;
    private String kafkaBootstrapServers;


    /**
     * Retrieves and initializes a RuntimeEnvironment object populated with configuration settings
     * for Kafka, Redis, and RabbitMQ, using corresponding environment variables.
     * If environment variables are not set, default values are assigned.
     *
     * @return a RuntimeEnvironment instance with the configured settings for Kafka, Redis, and RabbitMQ.
     */
    public static RuntimeEnvironment getEnvironment() {
        RuntimeEnvironment settings = new RuntimeEnvironment();

        settings.setKafkaBootstrapServers(System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENV_VAR) == null ? "localhost:9092" : System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENV_VAR));
        settings.setRedisHost(System.getenv(REDIS_HOST_ENV_VAR) == null ? "localhost" : System.getenv(REDIS_HOST_ENV_VAR));
        settings.setRedisPort(System.getenv(REDIS_PORT_ENV_VAR) == null ? 6379 : Integer.parseInt(System.getenv(REDIS_PORT_ENV_VAR)));
        settings.setRabbitmqHost(System.getenv(RABBITMQ_HOST_ENV_VAR) == null ? "localhost" : System.getenv(RABBITMQ_HOST_ENV_VAR));
        settings.setRabbitmqPort(System.getenv(RABBITMQ_PORT_ENV_VAR) == null ? 5672 : Integer.parseInt(System.getenv(RABBITMQ_PORT_ENV_VAR)));

        return settings;
    }
}
