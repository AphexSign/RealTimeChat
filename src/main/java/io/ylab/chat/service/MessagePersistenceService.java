package io.ylab.chat.service;

import io.ylab.chat.entity.MessageEntity;

/**
 * Service interface for asynchronously persisting {@link MessageEntity} instances.
 */
public interface MessagePersistenceService {

    /**
     * Asynchronously saves the given {@link MessageEntity} to the database within a transactional
     * context.
     *
     * @param entity the {@link MessageEntity} to be saved asynchronously
     * @throws RuntimeException if the save operation fails
     */
    void saveAsync(MessageEntity entity);

}