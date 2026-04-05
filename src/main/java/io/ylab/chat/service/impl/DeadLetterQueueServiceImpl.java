package io.ylab.chat.service.impl;

import io.micrometer.observation.annotation.Observed;
import io.ylab.chat.config.redis.RedisConstants;
import io.ylab.chat.entity.MessageEntity;
import io.ylab.chat.service.DeadLetterQueueService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link DeadLetterQueueService} that stores failed messages in a Redis Stream
 * for later inspection and replay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueServiceImpl implements DeadLetterQueueService {

    private final RedisTemplate<String, MessageEntity> messageEntityRedisTemplate;

    /**
     * {@inheritDoc}
     */
    @Observed(name = "dlq.pushFailedMessage")
    @Override
    public void pushFailedMessage(MessageEntity entity, Throwable error) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("messageId", entity.getMessageId());
            fields.put("sender", entity.getSender());
            fields.put("text", entity.getText());
            fields.put("timestamp",
                entity.getTimestamp() != null ? entity.getTimestamp().toString() : null);
            fields.put("error", error.getMessage());
            fields.put("failedAt", Instant.now().toString());
            messageEntityRedisTemplate.opsForStream()
                .add(RedisConstants.DEAD_LETTER_STREAM_KEY, fields);
            log.info("Failed message pushed to DLQ stream: {}", entity.getMessageId());
        } catch (Exception e) {
            log.error("Failed to push message to DLQ: {}", entity.getMessageId(), e);
        }
    }
}