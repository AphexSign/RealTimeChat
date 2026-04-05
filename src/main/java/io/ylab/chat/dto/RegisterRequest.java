package io.ylab.chat.dto;

import lombok.*;

/**
 * Data transfer object representing a user registration request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * The desired username for the new user account.
     */
    private String username;

    /**
     * The password for the new user account.
     */
    private String password;
}