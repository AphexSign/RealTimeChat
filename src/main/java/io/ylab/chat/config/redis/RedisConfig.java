package io.ylab.chat.config.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.ylab.chat.entity.MessageEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration class for Redis connections and serialization. This class sets up a reactive
 * Lettuce-based Redis client and two {@link RedisTemplate} beans for generic object storage and
 * typed message entity storage. Connection parameters (host, port, password) are read from Spring
 * environment properties with sensible defaults.
 *
 * @see RedisClient
 * @see RedisTemplate
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates a Lettuce-based Redis client with the configured host, port, and optional password.
     * The client is responsible for managing low-level connections to the Redis server. The
     * {@code destroyMethod = "shutdown"} ensures proper resource cleanup when the application
     * context is closed.
     *
     * @return a configured {@link RedisClient} instance
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient lettuceRedisClient() {
        RedisURI.Builder builder = RedisURI.builder()
            .withHost(redisHost)
            .withPort(redisPort);
        if (!redisPassword.isBlank()) {
            builder.withPassword(redisPassword.toCharArray());
        }
        return RedisClient.create(builder.build());
    }

    /**
     * Creates a generic {@link RedisTemplate} for storing and retrieving arbitrary Java objects.
     * Keys and hash keys are serialized as UTF-8 strings using {@link StringRedisSerializer}.
     * Values and hash values are serialized as JSON using {@link Jackson2JsonRedisSerializer} with
     * {@link Object} as the target type, allowing storage of any serializable object. This template
     * is suitable for general-purpose caching or data storage where the exact value type may vary.
     *
     * @param factory the Redis connection factory (auto-configured by Spring Boot)
     * @return a configured {@link RedisTemplate} for {@code String, Object} pairs
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(
            Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setStringSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * Creates a type-safe {@link RedisTemplate} specifically for {@link MessageEntity} objects.
     * <p>
     * Keys and hash keys are serialized as UTF-8 strings. Values and hash values are serialized as
     * JSON using a {@link Jackson2JsonRedisSerializer} parameterized with {@link MessageEntity},
     * ensuring type safety and eliminating the need for casting when working with message data.
     *
     * @param factory the Redis connection factory (auto-configured by Spring Boot)
     * @return a configured {@link RedisTemplate} for {@code String, MessageEntity} pairs
     */
    @Bean
    public RedisTemplate<String, MessageEntity> messageEntityRedisTemplate(
        RedisConnectionFactory factory) {
        RedisTemplate<String, MessageEntity> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<MessageEntity> serializer = new Jackson2JsonRedisSerializer<>(
            MessageEntity.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setStringSerializer(new StringRedisSerializer());
        return template;
    }
}