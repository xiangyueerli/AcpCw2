package uk.ac.ed.acp.cw2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

@RestController()
public class ServiceController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
    private final RuntimeEnvironment environment;

    public ServiceController(RuntimeEnvironment environment) {
        this.environment = environment;
    }


    @GetMapping("/")
    public String index() {
        StringBuilder currentEnv = new StringBuilder();
        currentEnv.append("<ul>");
        System.getenv().keySet().forEach(key -> currentEnv.append("<li>").append(key).append(" = ").append(System.getenv(key)).append("</li>"));
        currentEnv.append("</ul>");

        return "<html><body>" +
                "<h1>Welcome from ACP CW2</h1>" +
                "<h2>Environment variables </br><div> " + currentEnv.toString() + "</div></h2>" +
                "</body></html>";
    }

    @GetMapping("/uuid")
    public String uuid() {
        return "s12345678";
    }

    @GetMapping("/cache/{redisKey}")
    public String retrieveFromCache(@PathVariable String redisKey) {
        logger.info(String.format("Retrieving %s from cache", redisKey));
        try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); Jedis jedis = pool.getResource()) {
            logger.info("Redis connection established");

            String result = null;
            if (jedis.exists(redisKey)) {
                result = jedis.get(redisKey);
            }
            return result;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @PutMapping("/cache/{redisKey}/{redisValue}")
    public void storeInCache(@PathVariable String redisKey, @PathVariable String redisValue) {
        logger.info(String.format("Storing %s in cache with key %s", redisValue, redisKey));
        try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); Jedis jedis = pool.getResource()) {
            jedis.set(redisKey, redisValue);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }
}
