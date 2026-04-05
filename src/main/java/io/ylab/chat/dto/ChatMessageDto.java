package io.ylab.chat.dto;

import lombok.*;

/**
 * Data transfer object representing a chat message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    /**
     * The unique identifier of the message.
     */
    private String messageId;

    /**
     * The username or identifier of the sender of the message.
     */
    private String sender;

    /**
     * The text content of the message.
     */
    private String text;

    /**
     * The timestamp of the message represented as milliseconds since the epoch (Unix time).
     */
    private Long timestamp;
}