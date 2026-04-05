package io.ylab.chat.config;

import io.ylab.chat.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.security.Principal;
import java.util.ArrayList;

/**
 * Configuration class for setting up WebSocket messaging with Spring.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    /**
     * Configures the message broker for WebSocket communication.
     *
     * @param config the {@link MessageBrokerRegistry} to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registers STOMP endpoints for WebSocket communication.
     *
     * @param registry the {@link StompEndpointRegistry} to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }

    private Principal authenticateFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtUtil.extractUsername(token);
            if (jwtUtil.validateToken(token, username)) {
                return () -> username;
            }
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Configures the client inbound channel to intercept and process WebSocket messages.
     *
     * @param registration the {@link ChannelRegistration} to configure
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
                    StompHeaderAccessor.class);

                log.debug("WebSocket Interceptor: Command = {}, User = {}",
                    accessor.getCommand(), accessor.getUser());

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authToken = accessor.getFirstNativeHeader("Authorization");
                    Principal principal = authenticateFromToken(authToken);

                    if (principal != null) {
                        accessor.setUser(principal);
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal.getName(), null,
                                new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        throw new RuntimeException("Invalid JWT token");
                    }
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                    StompCommand.SEND.equals(accessor.getCommand())) {

                    if (accessor.getUser() == null) {
                        Authentication auth = SecurityContextHolder.getContext()
                            .getAuthentication();
                        if (auth != null && auth.isAuthenticated()
                            && !(auth.getPrincipal() instanceof String)) {
                            Principal principal = () -> auth.getName();
                            accessor.setUser(principal);
                        }
                    }

                    if (accessor.getUser() == null) {
                        String authToken = accessor.getFirstNativeHeader("Authorization");
                        if (authToken != null && authToken.startsWith("Bearer ")) {
                            String token = authToken.substring(7);
                            try {
                                String username = jwtUtil.extractUsername(token);
                                if (jwtUtil.validateToken(token, username)) {
                                    Principal principal = () -> username;
                                    accessor.setUser(principal);
                                    UsernamePasswordAuthenticationToken authTokenObj =
                                        new UsernamePasswordAuthenticationToken(principal, null,
                                            new ArrayList<>());
                                    SecurityContextHolder.getContext()
                                        .setAuthentication(authTokenObj);
                                }
                            } catch (Exception e) {
                                log.error("WebSocket {}: Invalid JWT token", accessor.getCommand(),
                                    e);
                            }
                        }
                    }
                }

                return message;
            }
        });
    }
}