package io.ylab.chat.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.ylab.chat.aop.annotation.Idempotent;
import io.ylab.chat.aop.annotation.RateLimited;
import io.ylab.chat.aop.annotation.WithUserContext;
import io.ylab.chat.context.UserContext;
import io.ylab.chat.dto.ChatMessageDto;
import io.ylab.chat.entity.MessageEntity;
import io.ylab.chat.service.ChatService;
import io.ylab.chat.service.MessagePersistenceService;
import io.ylab.chat.util.TimeProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Implementation of the {@link ChatService} interface responsible for handling chat message
 * operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessagePersistenceService persistenceService;
    private final TimeProvider timeProvider;
    private final MeterRegistry meterRegistry;
    private Counter messagesSentCounter;

    @PostConstruct
    public void init() {
        this.messagesSentCounter = Counter.builder("websocket.messages.sent")
            .description("Total number of messages sent to /topic/chat")
            .register(meterRegistry);
    }

    /**
     * {@inheritDoc}
     */
    @WithUserContext
    @RateLimited
    @Idempotent
    @Override
    public void sendMessage(ChatMessageDto dto) {
        String sender = UserContext.getCurrentUsername();
        LocalDateTime now = timeProvider.now();
        dto.setSender(sender);
        dto.setTimestamp(now.toInstant(ZoneOffset.UTC).toEpochMilli());

        MessageEntity entity = MessageEntity.builder()
            .messageId(dto.getMessageId())
            .sender(sender)
            .text(dto.getText())
            .timestamp(now)
            .build();

        persistenceService.saveAsync(entity);
        messagingTemplate.convertAndSend("/topic/chat", dto);

        messagesSentCounter.increment();

        log.info("Message sent from {} to /topic/chat", sender);
    }
}