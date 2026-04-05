package io.ylab.chat.service;

import io.ylab.chat.entity.MessageEntity;

/**
 * Service for managing dead-letter queue (DLQ) operations in the chat system.
 *
 * @see MessageEntity
 */
public interface DeadLetterQueueService {

    /**
     * Pushes a message that failed processing into the dead-letter queue.
     *
     * @param entity the message entity that failed to be processed (must not be {@code null})
     * @param error  the exception that caused the failure (must not be {@code null})
     */
    void pushFailedMessage(MessageEntity entity, Throwable error);

}