package io.ylab.chat.dto;

import lombok.*;

/**
 * Data transfer object representing the response returned after successful authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * The JSON Web Token (JWT) issued upon successful authentication.
     */
    private String token;

    /**
     * The username of the authenticated user.
     */
    private String username;
}