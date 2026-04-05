package io.ylab.chat.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterConfig {

    private final RedisClient redisClient;
    private final RateLimiterProperties properties;

    @Bean
    public ProxyManager<byte[]> bucketProxyManager() {
        StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(
            ByteArrayCodec.INSTANCE);
        return LettuceBasedProxyManager.builderFor(connection)
            .build();
    }

    @Bean
    public Supplier<BucketConfiguration> messageBucketConfiguration() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(properties.getMessages().getCapacity())
                .refillGreedy(properties.getMessages().getRefillTokens(),
                    Duration.ofSeconds(properties.getMessages().getRefillDurationSeconds()))
                .build())
            .build();
    }
}