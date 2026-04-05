package io.ylab.chat.dto;

import lombok.*;

/**
 * Data transfer object representing a login request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * The username of the user attempting to log in.
     */
    private String username;

    /**
     * The password of the user attempting to log in.
     */
    private String password;
}