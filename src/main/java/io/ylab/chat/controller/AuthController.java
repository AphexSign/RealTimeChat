package io.ylab.chat.controller;

import io.ylab.chat.dto.AuthResponse;
import io.ylab.chat.dto.LoginRequest;
import io.ylab.chat.dto.RegisterRequest;
import io.ylab.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 *
 * @see UserService
 * @see RegisterRequest
 * @see LoginRequest
 * @see AuthResponse
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Registers a new user.
     *
     * @param request the registration data (must not be {@code null})
     * @return a {@link ResponseEntity} containing {@link AuthResponse} with HTTP status 200 (OK) if
     * registration is successful
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Authenticates an existing user.
     *
     * @param request the login credentials (must not be {@code null})
     * @return a {@link ResponseEntity} containing {@link AuthResponse} with HTTP status 200 (OK) if
     * authentication succeeds
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }
}