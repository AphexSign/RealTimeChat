package io.ylab.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity representing a chat message stored in the database.
 */
@Entity
@Setter
@Getter
@Builder
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    /**
     * The primary key of the message entity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique message identifier.
     */
    @Column(unique = true, nullable = false)
    private String messageId;

    /**
     * The username or identifier of the sender of the message.
     */
    @Column(nullable = false)
    private String sender;

    /**
     * The text content of the message.
     */
    @Column(nullable = false, length = 2000)
    private String text;

    /**
     * The timestamp when the message was sent.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;
}