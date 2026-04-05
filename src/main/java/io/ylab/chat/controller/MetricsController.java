package io.ylab.chat.controller;

import io.ylab.chat.aop.DegradationAspect;
import io.ylab.chat.concurrency.OnlineUserRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * REST controller for system metrics and operational controls.
 *
 * @see OnlineUserRegistry
 * @see DegradationAspect
 */
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final OnlineUserRegistry onlineUserRegistry;
    private final DegradationAspect degradationAspect;

    /**
     * Manually recovers the system from degraded mode.
     *
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) and a confirmation message
     */
    @PostMapping("/recover")
    public ResponseEntity<String> recover() {
        degradationAspect.recover();
        return ResponseEntity.ok("System recovered from degraded mode");
    }

    /**
     * Retrieves the set of currently online users.
     *
     * @return a {@link ResponseEntity} containing a {@link Set} of online usernames with HTTP
     * status 200 (OK)
     */
    @GetMapping("/online")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(onlineUserRegistry.getOnlineUsers());
    }
}