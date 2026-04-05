package io.ylab.chat.service;

import io.ylab.chat.dto.AuthResponse;
import io.ylab.chat.dto.LoginRequest;
import io.ylab.chat.dto.RegisterRequest;

/**
 * Service interface for user authentication and registration operations.
 */
public interface UserService {

    /**
     * Registers a new user by validating the input, encoding the password, saving the user to the
     * database, and generating a JWT token for the newly registered user.
     *
     * @param request the {@link RegisterRequest} containing the user's registration details
     * @return an {@link AuthResponse} containing the generated JWT token and the username
     * @throws IllegalArgumentException if the username already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user by verifying their credentials and generating a JWT token upon
     * successful authentication.
     *
     * @param request the {@link LoginRequest} containing the user's login credentials
     * @return an {@link AuthResponse} containing the generated JWT token and the username
     * @throws IllegalArgumentException if the credentials are invalid
     */
    AuthResponse login(LoginRequest request);

}