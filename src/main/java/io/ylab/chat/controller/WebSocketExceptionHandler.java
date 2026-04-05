package io.ylab.chat.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles exceptions thrown during WebSocket message processing. This controller advice intercepts
 * exceptions raised by {@code @MessageMapping} methods and sends error responses back to the client
 * via a private user queue.
 */
@Controller
public class WebSocketExceptionHandler {

    /**
     * Handles {@link ResponseStatusException} thrown during message processing.
     *
     * @param ex the caught ResponseStatusException
     * @return the error reason to be delivered to the client
     */
    @MessageExceptionHandler(ResponseStatusException.class)
    @SendToUser("/queue/errors")
    public String handleRateLimit(ResponseStatusException ex) {
        return ex.getReason();
    }

    /**
     * Handles any other exception not specifically mapped.
     *
     * @param ex the caught exception
     * @return the exception message
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        return ex.getMessage();
    }
}