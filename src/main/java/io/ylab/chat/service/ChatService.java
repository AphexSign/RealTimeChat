package io.ylab.chat.service;

import io.ylab.chat.dto.ChatMessageDto;

/**
 * Service interface for handling chat message operations in a real-time chat application.
 */
public interface ChatService {

    /**
     * Sends a chat message by setting the sender and timestamp, persisting the message
     * asynchronously, and broadcasting it to all subscribers of the "/topic/chat" WebSocket topic.
     *
     * @param dto the {@link ChatMessageDto} containing the message details to be sent
     */
    void sendMessage(ChatMessageDto dto);

}