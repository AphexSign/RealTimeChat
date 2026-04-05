package io.ylab.chat.config.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes Redis stream infrastructure components on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStreamInitializer {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Creates the dead-letter queue consumer group in Redis. Invoked automatically after dependency
     * injection is complete. If the group creation fails because the group already exists (common
     * on subsequent application restarts), the exception is logged at debug level. For any other
     * exception, the same debug log is written; the method does not rethrow the exception to avoid
     * blocking startup.
     */
    @PostConstruct
    public void init() {
        try {
            redisTemplate.opsForStream().createGroup(RedisConstants.DEAD_LETTER_STREAM_KEY,
                RedisConstants.DEAD_LETTER_CONSUMER_GROUP);
            log.info("Redis Stream consumer group '{}' created for key '{}'",
                RedisConstants.DEAD_LETTER_CONSUMER_GROUP, RedisConstants.DEAD_LETTER_STREAM_KEY);
        } catch (Exception e) {
            log.debug("Redis Stream group '{}' already exists or error: {}",
                RedisConstants.DEAD_LETTER_CONSUMER_GROUP, e.getMessage());
        }
    }
}