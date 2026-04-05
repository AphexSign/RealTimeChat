package io.ylab.chat.repository;

import io.ylab.chat.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link MessageEntity} persistence operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    /**
     * Checks whether a message with the specified message ID exists in the repository.
     *
     * @param messageId the unique identifier of the message to check
     * @return {@code true} if a message with the given ID exists, {@code false} otherwise
     */
    boolean existsByMessageId(String messageId);
}