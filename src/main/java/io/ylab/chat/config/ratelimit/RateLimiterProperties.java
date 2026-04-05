package io.ylab.chat.config.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private Messages messages = new Messages();

    @Getter
    @Setter
    public static class Messages {
        private long capacity;
        private long refillTokens;
        private long refillDurationSeconds;
    }
}