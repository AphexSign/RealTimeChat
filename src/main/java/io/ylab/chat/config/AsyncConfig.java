package io.ylab.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class for setting up asynchronous task execution.
 */
@Configuration
@Slf4j
public class AsyncConfig {

    /**
     * Creates and configures a {@link ThreadPoolTaskExecutor} for chat-related asynchronous tasks.
     *
     * @return the configured {@link ThreadPoolTaskExecutor} instance
     */
    @Bean(name = "chatExecutor")
    public ThreadPoolTaskExecutor chatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("chat-msg-");
        executor.setRejectedExecutionHandler(
            new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadFactory(r -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler((thread, e) ->
                log.error("Uncaught exception in thread {}: {}", thread.getName(), e.getMessage(),
                    e));
            return t;
        });
        executor.initialize();
        return executor;
    }
}