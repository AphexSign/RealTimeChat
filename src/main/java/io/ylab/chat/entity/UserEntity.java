package io.ylab.chat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a user in the system.
 */
@Entity
@Setter
@Getter
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /**
     * The primary key of the user entity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique username of the user.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * The hashed password of the user.
     */
    @Column(nullable = false)
    private String passwordHash;
}