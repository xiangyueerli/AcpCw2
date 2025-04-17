package uk.ac.ed.acp.cw2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import uk.ac.ed.acp.cw2.data.RuntimeEnvironment;

/**
 * Controller class responsible for handling REST endpoints for managing
 * cache storage using Redis. Provides functionality to retrieve and store
 * key-value pairs in the cache.
 */
@Slf4j
@Service
public class CacheService {

    private final RuntimeEnvironment environment;

    public CacheService(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    //
    public String retrieveFromCache(String cacheKey) {
        log.info(String.format("Retrieving %s from cache", cacheKey));
        try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); Jedis jedis = pool.getResource()) {
            log.info("Redis connection established");

            String result = null;
            if (jedis.exists(cacheKey)) {
                result = jedis.get(cacheKey);
            }
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    public void storeInCache(String cacheKey, String cacheValue) {
        log.info(String.format("Storing %s in cache with key %s", cacheValue, cacheKey));
        try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort()); Jedis jedis = pool.getResource()) {
            jedis.set(cacheKey, cacheValue);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    public void removeKey(String cacheKey) {
        log.info(String.format("Removing key %s from cache", cacheKey));
        try (JedisPool pool = new JedisPool(environment.getRedisHost(), environment.getRedisPort());
             Jedis jedis = pool.getResource()) {
            jedis.del(cacheKey);
            log.info("Key {} removed successfully", cacheKey);
        } catch (Exception e) {
            log.error("Error removing key from Redis: {}", e.getMessage(), e);
            throw e;
        }
    }

}
