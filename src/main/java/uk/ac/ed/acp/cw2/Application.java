package uk.ac.ed.acp.cw2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    private static RuntimeEnvironmentSettings checkEnvironment() {
        RuntimeEnvironmentSettings settings = new RuntimeEnvironmentSettings();

        if (System.getenv("KAFKA_BOOTSTRAP_SERVERS") == null) {
            throw new RuntimeException("KAFKA_BOOTSTRAP_SERVERS environment variable not set");
        }
        settings.setKafkaBootstrapServers(System.getenv("KAFKA_BOOTSTRAP_SERVERS"));

        if (System.getenv("REDIS_HOST") == null) {
            throw new RuntimeException("REDIS_HOST environment variable not set");
        }
        settings.setRedisHost(System.getenv("REDIS_HOST"));

        if (System.getenv("REDIS_PORT") == null) {
            throw new RuntimeException("REDIS_PORT environment variable not set");
        }
        settings.setRedisPort(Integer.parseInt(System.getenv("REDIS_PORT")));

        if (System.getenv("RABBITMQ_HOST") == null) {
            throw new RuntimeException("RABBITMQ_HOST environment variable not set");
        }
        settings.setRabbitmqHost(System.getenv("RABBITMQ_HOST"));

        if (System.getenv("RABBITMQ_PORT") == null) {
            throw new RuntimeException("RABBITMQ_PORT environment variable not set");
        }
        settings.setRabbitmqPort(Integer.parseInt(System.getenv("RABBITMQ_PORT")));

        return settings;
    }
}
