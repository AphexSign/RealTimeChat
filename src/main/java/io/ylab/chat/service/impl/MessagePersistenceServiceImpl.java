package io.ylab.chat.service.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.observation.annotation.Observed;
import io.ylab.chat.entity.MessageEntity;
import io.ylab.chat.repository.MessageRepository;
import io.ylab.chat.service.DeadLetterQueueService;
import io.ylab.chat.service.MessagePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link MessagePersistenceService} responsible for asynchronously persisting
 * {@link MessageEntity} instances to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePersistenceServiceImpl implements MessagePersistenceService {

    private final MessageRepository messageRepository;

    private final DeadLetterQueueService deadLetterQueueService;

    /**
     * {@inheritDoc}
     */
    @CircuitBreaker(name = "messagePersistence", fallbackMethod = "fallbackSave")
    @Async("chatExecutor")
    @Transactional
    @Override
    @Observed(name = "persistence.saveAsync", contextualName = "save-message-async")
    public void saveAsync(MessageEntity entity) {
        try {
            messageRepository.save(entity);
            log.debug("Message saved asynchronously: {}", entity.getMessageId());
        } catch (Exception e) {
            log.error("Failed to save message {}: {}", entity.getMessageId(), e.getMessage());
            throw e;
        }
    }
}