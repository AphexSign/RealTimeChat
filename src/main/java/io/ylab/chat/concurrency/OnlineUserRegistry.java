package io.ylab.chat.concurrency;

import lombok.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component for managing the registry of online users in a thread-safe manner.
 */
@Component
public class OnlineUserRegistry {

    /**
     * A thread-safe map to store online users and their session information. The key is the
     * username, and the value is a {@link SessionInfo} object.
     */
    private final Map<String, SessionInfo> onlineUsers = new ConcurrentHashMap<>();

    /**
     * Registers a user as online by storing their session information.
     *
     * @param username  the username of the user
     * @param sessionId the session ID associated with the user
     */
    public void register(String username, String sessionId) {
        SessionInfo info = SessionInfo.builder()
            .username(username)
            .sessionId(sessionId)
            .connectedAt(System.currentTimeMillis())
            .build();
        onlineUsers.put(username, info);
    }

    /**
     * Unregisters a user, removing them from the online users registry.
     *
     * @param username the username of the user to unregister
     */
    public void unregister(String username) {
        onlineUsers.remove(username);
    }

    /**
     * Checks if a user is currently online.
     *
     * @param username the username to check
     * @return {@code true} if the user is online, {@code false} otherwise
     */
    public boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    /**
     * Retrieves the set of usernames of all online users.
     *
     * @return a {@link Set} containing the usernames of online users
     */
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers.keySet());
    }

    /**
     * Retrieves the total count of online users.
     *
     * @return the number of online users
     */
    public int getOnlineCount() {
        return onlineUsers.size();
    }

    /**
     * Inner class representing session information for an online user.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {

        /**
         * The username of the user.
         */
        private String username;

        /**
         * The session ID associated with the user.
         */
        private String sessionId;

        /**
         * The timestamp (in milliseconds) when the user connected.
         */
        private long connectedAt;
    }
}