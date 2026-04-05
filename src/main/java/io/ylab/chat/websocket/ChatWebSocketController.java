package io.ylab.chat.websocket;

import io.ylab.chat.dto.ChatMessageDto;
import io.ylab.chat.exception.RateLimitExceededException;
import io.ylab.chat.exception.UnauthorizedException;
import io.ylab.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.ArrayList;

/**
 * The {@code ChatWebSocketController} class handles WebSocket messages related to chat
 * functionality.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles incoming chat messages sent to the "/chat.send" WebSocket destination.
     *
     * @param message        the {@link ChatMessageDto} payload containing the chat message
     *                       details.
     * @param principal      the {@link Principal} representing the authenticated user (can be
     *                       null).
     * @param headerAccessor the {@link SimpMessageHeaderAccessor} providing access to WebSocket
     *                       headers.
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto message,
        Principal principal,
        SimpMessageHeaderAccessor headerAccessor) {
        try {
            if (principal != null) {
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal.getName(), null,
                        new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("ChatWebSocketController: SecurityContext set for user {}",
                    principal.getName());
            } else {
                log.warn("ChatWebSocketController: Principal is null!");
            }

            chatService.sendMessage(message);

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized message attempt: {}", e.getMessage());
            sendErrorToUser(headerAccessor, "Unauthorized: " + e.getMessage());

        } catch (RateLimitExceededException e) {
            log.warn("Rate limit exceeded: {}", e.getMessage());
            sendErrorToUser(headerAccessor, "Rate limit: " + e.getMessage());

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage(), e);
            sendErrorToUser(headerAccessor, "Error: Message could not be sent");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Sends an error message to the user via a private WebSocket destination.
     *
     * @param headerAccessor the {@link SimpMessageHeaderAccessor} providing access to WebSocket
     *                       headers.
     * @param errorMessage   the error message to be sent to the user.
     */
    private void sendErrorToUser(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        String sessionId = headerAccessor.getSessionId();
        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/errors",
            errorMessage
        );
    }
}