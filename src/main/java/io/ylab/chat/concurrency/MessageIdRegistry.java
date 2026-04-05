package io.ylab.chat.concurrency;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Component that tracks processed message IDs to prevent duplicate processing.
 */
@Component
public class MessageIdRegistry {

    /**
     * Time-to-live for message IDs in milliseconds (5 minutes).
     */
    private static final long TTL_MS = 300_000;

    /**
     * Map storing processed message IDs and their processing timestamps.
     */
    private final Map<String, Long> processedMessages = new ConcurrentHashMap<>();
    /**
     * Scheduled executor service for periodic cleanup of expired message IDs.
     */
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructs a new {@code MessageIdRegistry} and starts the periodic cleanup task.
     */
    public MessageIdRegistry() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Checks if a message ID has already been processed.
     *
     * @param messageId the message ID to check
     * @return {@code true} if the message ID has been processed, {@code false} otherwise
     */
    public boolean isProcessed(String messageId) {
        return processedMessages.containsKey(messageId);
    }

    /**
     * Marks a message ID as processed by recording the current timestamp.
     *
     * @param messageId the message ID to mark as processed
     * @return {@code true} if the message ID was newly recorded, {@code false} if it was already
     * present
     */
    public boolean markAsProcessed(String messageId) {
        Long existing = processedMessages.putIfAbsent(messageId, System.currentTimeMillis());
        return existing == null;
    }

    /**
     * Removes message IDs that have exceeded their time-to-live (TTL).
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        processedMessages.entrySet().removeIf(entry -> now - entry.getValue() > TTL_MS);
    }

    /**
     * Shuts down the scheduled cleanup executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}