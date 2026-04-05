package io.ylab.chat.context;

import lombok.*;

/**
 * Data transfer object representing basic user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * The username of the user.
     */
    private String username;

    /**
     * The unique identifier of the user.
     */
    private Long userId;
}