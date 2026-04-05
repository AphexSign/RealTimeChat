package io.ylab.chat.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.ylab.chat.context.UserContext;
import io.ylab.chat.context.UserInfo;
import io.ylab.chat.dto.ChatMessageDto;
import io.ylab.chat.entity.MessageEntity;
import io.ylab.chat.service.MessagePersistenceService;
import io.ylab.chat.util.TimeProvider;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("ChatServiceImpl Tests")
class ChatServiceImplTest {

    private static final String TEST_USERNAME = "john_doe";
    private static final String MESSAGE_ID = "msg-123";
    private static final String MESSAGE_TEXT = "Hello";
    private static final String CHAT_DESTINATION = "/topic/chat";
    private static final String METRIC_MESSAGES_SENT = "websocket.messages.sent";
    private static final int TEST_YEAR = 2025;
    private static final int TEST_MONTH = 4;
    private static final int TEST_DAY = 5;
    private static final int TEST_HOUR = 12;
    private static final int TEST_MINUTE = 0;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessagePersistenceService persistenceService;

    @Mock
    private TimeProvider timeProvider;

    @InjectSoftAssertions
    private SoftAssertions softly;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(messagingTemplate, persistenceService, timeProvider,
            meterRegistry);
        chatService.init();
    }

    @Test
    @DisplayName("Send message successfully")
    void sendMessage_Success() {
        UserContext.setUserInfo(UserInfo.builder().username(TEST_USERNAME).build());
        LocalDateTime now = LocalDateTime.of(TEST_YEAR, TEST_MONTH, TEST_DAY, TEST_HOUR, TEST_MINUTE);
        when(timeProvider.now()).thenReturn(now);

        ChatMessageDto dto = ChatMessageDto.builder()
            .messageId(MESSAGE_ID)
            .text(MESSAGE_TEXT)
            .build();

        chatService.sendMessage(dto);

        softly.assertThat(dto.getSender()).isEqualTo(TEST_USERNAME);
        softly.assertThat(dto.getTimestamp()).isEqualTo(now.toInstant(ZoneOffset.UTC).toEpochMilli());

        ArgumentCaptor<MessageEntity> entityCaptor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(persistenceService).saveAsync(entityCaptor.capture());
        MessageEntity savedEntity = entityCaptor.getValue();
        softly.assertThat(savedEntity.getMessageId()).isEqualTo(MESSAGE_ID);
        softly.assertThat(savedEntity.getSender()).isEqualTo(TEST_USERNAME);
        softly.assertThat(savedEntity.getText()).isEqualTo(MESSAGE_TEXT);
        softly.assertThat(savedEntity.getTimestamp()).isEqualTo(now);

        verify(messagingTemplate).convertAndSend(CHAT_DESTINATION, dto);

        var counter = meterRegistry.find(METRIC_MESSAGES_SENT).counter();
        softly.assertThat(counter).isNotNull();
        softly.assertThat(Objects.requireNonNull(counter).count()).isEqualTo(1);
        UserContext.clear();
    }
}