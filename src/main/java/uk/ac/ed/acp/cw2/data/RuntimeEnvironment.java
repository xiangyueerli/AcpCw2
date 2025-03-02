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
     * Checks the environment for the required variables and initializes a RuntimeEnvironment object
     * with those values if they are present. Throws a RuntimeException if any required environment variable
     * is missing.
     *
     * @return A RuntimeEnvironment object containing the configuration values extracted from the environment.
     */
    public static RuntimeEnvironment getEnvironment() {
        RuntimeEnvironment settings = new RuntimeEnvironment();

        if (System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENV_VAR) == null) {
            throw new RuntimeException("KAFKA_BOOTSTRAP_SERVERS environment variable not set");
        }
        settings.setKafkaBootstrapServers(System.getenv(KAFKA_BOOTSTRAP_SERVERS_ENV_VAR));

        if (System.getenv(REDIS_HOST_ENV_VAR) == null) {
            throw new RuntimeException("REDIS_HOST environment variable not set");
        }
        settings.setRedisHost(System.getenv(REDIS_HOST_ENV_VAR));

        if (System.getenv(REDIS_PORT_ENV_VAR) == null) {
            throw new RuntimeException("REDIS_PORT environment variable not set");
        }
        settings.setRedisPort(Integer.parseInt(System.getenv(REDIS_PORT_ENV_VAR)));

        if (System.getenv(RABBITMQ_HOST_ENV_VAR) == null) {
            throw new RuntimeException("RABBITMQ_HOST environment variable not set");
        }
        settings.setRabbitmqHost(System.getenv(RABBITMQ_HOST_ENV_VAR));

        if (System.getenv(RABBITMQ_PORT_ENV_VAR) == null) {
            throw new RuntimeException("RABBITMQ_PORT environment variable not set");
        }
        settings.setRabbitmqPort(Integer.parseInt(System.getenv(RABBITMQ_PORT_ENV_VAR)));

        return settings;
    }
}
