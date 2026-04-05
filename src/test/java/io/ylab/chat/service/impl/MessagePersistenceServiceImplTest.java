package io.ylab.chat.service.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import io.ylab.chat.entity.MessageEntity;
import io.ylab.chat.repository.MessageRepository;
import io.ylab.chat.service.DeadLetterQueueService;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("Message Persistence Service Tests")
class MessagePersistenceServiceImplTest {

    private static final String MESSAGE_ID = "123";
    private static final String SENDER_ALICE = "alice";
    private static final String MESSAGE_TEXT = "Hi";
    private static final String DATABASE_ERROR_MESSAGE = "DB down";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private DeadLetterQueueService deadLetterQueueService;

    @InjectMocks
    private MessagePersistenceServiceImpl persistenceService;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    @DisplayName("Save message asynchronously - success")
    void saveAsync_Success() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(MESSAGE_ID)
            .sender(SENDER_ALICE)
            .text(MESSAGE_TEXT)
            .build();

        persistenceService.saveAsync(entity);

        verify(messageRepository).save(entity);
        verifyNoInteractions(deadLetterQueueService);
    }

    @Test
    @DisplayName("Save message asynchronously - exception propagates")
    void saveAsync_Exception_ThrowsException() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(MESSAGE_ID)
            .sender(SENDER_ALICE)
            .text(MESSAGE_TEXT)
            .build();
        doThrow(new RuntimeException(DATABASE_ERROR_MESSAGE)).when(messageRepository).save(entity);

        softly.assertThatThrownBy(() -> persistenceService.saveAsync(entity))
            .isInstanceOf(RuntimeException.class)
            .hasMessage(DATABASE_ERROR_MESSAGE);

        verifyNoInteractions(deadLetterQueueService);
    }
}