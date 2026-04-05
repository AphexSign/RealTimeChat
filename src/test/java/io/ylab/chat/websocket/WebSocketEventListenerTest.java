package io.ylab.chat.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.ylab.chat.concurrency.OnlineUserRegistry;
import java.security.Principal;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("WebSocketEventListener Tests")
class WebSocketEventListenerTest {

    private static final String USERNAME_ALICE = "alice";
    private static final String USERNAME_BOB = "bob";
    private static final String SESSION_ID = "sess1";
    private static final String CHAT_DESTINATION = "/topic/chat";
    private static final String USER_JOINED_TYPE = "USER_JOINED";
    private static final String USER_LEFT_TYPE = "USER_LEFT";
    private static final String TYPE_FIELD = "type";
    private static final String USERNAME_FIELD = "username";
    private static final String ONLINE_COUNT_FIELD = "onlineCount";
    private static final String METRIC_NAME = "websocket.connections.total";
    private static final String METRIC_TAG_EVENT = "event";
    private static final String CONNECT_EVENT = "connect";
    private static final String DISCONNECT_EVENT = "disconnect";

    @Mock
    private OnlineUserRegistry onlineUserRegistry;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MeterRegistry meterRegistry;

    private WebSocketEventListener listener;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new WebSocketEventListener(onlineUserRegistry, messagingTemplate, meterRegistry);
        listener.init();
    }

    @Test
    @DisplayName("On user connect, registers user and sends USER_JOINED message")
    void handleWebSocketConnectListener_registersUserAndSendsJoinMessage() {
        SessionConnectEvent event = mock(SessionConnectEvent.class);
        Message<byte[]> message = mock(Message.class);
        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        Principal principal = mock(Principal.class);

        when(event.getMessage()).thenReturn(message);
        when(principal.getName()).thenReturn(USERNAME_ALICE);
        when(accessor.getUser()).thenReturn(principal);
        when(accessor.getSessionId()).thenReturn(SESSION_ID);

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(message)).thenReturn(accessor);

            listener.handleWebSocketConnectListener(event);

            verify(onlineUserRegistry).register(USERNAME_ALICE, SESSION_ID);

            verify(messagingTemplate).convertAndSend(
                eq(CHAT_DESTINATION),
                ArgumentMatchers.<Object>argThat(payload -> {
                    if (!(payload instanceof Map<?, ?> map)) {
                        return false;
                    }
                    return USER_JOINED_TYPE.equals(map.get(TYPE_FIELD))
                        && USERNAME_ALICE.equals(map.get(USERNAME_FIELD))
                        && map.containsKey(ONLINE_COUNT_FIELD);
                })
            );

            softly.assertThat(meterRegistry.find(METRIC_NAME)
                    .tag(METRIC_TAG_EVENT, CONNECT_EVENT)
                    .counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0));
        }
    }

    @Test
    @DisplayName("On user disconnect, removes user and sends USER_LEFT message")
    void handleWebSocketDisconnectListener_unregistersAndSendsLeaveMessage() {
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        Message<byte[]> message = mock(Message.class);
        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        Principal principal = mock(Principal.class);

        when(event.getMessage()).thenReturn(message);
        when(principal.getName()).thenReturn(USERNAME_BOB);
        when(accessor.getUser()).thenReturn(principal);

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(message)).thenReturn(accessor);

            listener.handleWebSocketDisconnectListener(event);

            verify(onlineUserRegistry).unregister(USERNAME_BOB);

            verify(messagingTemplate).convertAndSend(
                eq(CHAT_DESTINATION),
                ArgumentMatchers.<Object>argThat(payload -> {
                    if (!(payload instanceof Map<?, ?> map)) {
                        return false;
                    }
                    return USER_LEFT_TYPE.equals(map.get(TYPE_FIELD))
                        && USERNAME_BOB.equals(map.get(USERNAME_FIELD))
                        && map.containsKey(ONLINE_COUNT_FIELD);
                })
            );

            softly.assertThat(meterRegistry.find(METRIC_NAME)
                    .tag(METRIC_TAG_EVENT, DISCONNECT_EVENT)
                    .counter())
                .isNotNull()
                .satisfies(counter -> assertThat(counter.count()).isEqualTo(1.0));
        }
    }
}