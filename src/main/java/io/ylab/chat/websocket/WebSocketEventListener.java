package io.ylab.chat.websocket;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.ylab.chat.concurrency.OnlineUserRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code WebSocketEventListener} class listens for WebSocket connection and disconnection
 * events and handles user registration and notification broadcasting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineUserRegistry onlineUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    private Counter connectCounter;
    private Counter disconnectCounter;

    @PostConstruct
    public void init() {
        connectCounter = Counter.builder("websocket.connections.total")
            .tag("event", "connect")
            .register(meterRegistry);
        disconnectCounter = Counter.builder("websocket.connections.total")
            .tag("event", "disconnect")
            .register(meterRegistry);
    }

    /**
     * Handles WebSocket connection events.
     *
     * @param event the {@link SessionConnectEvent} triggered when a WebSocket connection is
     *              established.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        if (user != null) {
            String username = user.getName();
            onlineUserRegistry.register(username, sessionId);

            log.info("User connected: {} (session: {})", username, sessionId);

            Map<String, Object> joinMessage = new HashMap<>();
            joinMessage.put("type", "USER_JOINED");
            joinMessage.put("username", username);
            joinMessage.put("onlineCount", onlineUserRegistry.getOnlineCount());

            messagingTemplate.convertAndSend("/topic/chat", joinMessage);

            connectCounter.increment();

        }
    }

    /**
     * Handles WebSocket disconnection events.
     *
     * @param event the {@link SessionDisconnectEvent} triggered when a WebSocket connection is
     *              closed.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();

            onlineUserRegistry.unregister(username);

            log.info("User disconnected: {}", username);

            Map<String, Object> leaveMessage = new HashMap<>();
            leaveMessage.put("type", "USER_LEFT");
            leaveMessage.put("username", username);
            leaveMessage.put("onlineCount", onlineUserRegistry.getOnlineCount());

            messagingTemplate.convertAndSend("/topic/chat", leaveMessage);

            disconnectCounter.increment();
        }
    }
}