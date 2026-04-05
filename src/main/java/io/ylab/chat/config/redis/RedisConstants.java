package io.ylab.chat.config.redis;

import lombok.experimental.UtilityClass;

/**
 * Central repository of Redis key and consumer group constants used throughout the chat
 * application.
 *
 * @see org.springframework.data.redis.connection.stream.StreamRecords
 * @see org.springframework.data.redis.connection.stream.Consumer
 */
@UtilityClass
public class RedisConstants {

    /**
     * Redis stream key for the dead-letter queue.
     */
    public static final String DEAD_LETTER_STREAM_KEY = "chat:dead-letter:stream";

    /**
     * Consumer group name for the dead-letter queue stream.
     */
    public static final String DEAD_LETTER_CONSUMER_GROUP = "chat-dlq-group";

}