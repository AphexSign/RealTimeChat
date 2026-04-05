package io.ylab.chat.service.impl;

import io.ylab.chat.dto.AuthResponse;
import io.ylab.chat.dto.LoginRequest;
import io.ylab.chat.dto.RegisterRequest;
import io.ylab.chat.entity.UserEntity;
import io.ylab.chat.repository.UserRepository;
import io.ylab.chat.service.UserService;
import io.ylab.chat.util.JwtUtil;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link UserService} interface, providing user registration and login
 * functionality.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        UserEntity user = UserEntity.builder()
            .username(request.getUsername())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();

        userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            Collections.emptyList()
        );

        String token = jwtUtil.generateToken(authentication);

        return AuthResponse.builder()
            .token(token)
            .username(user.getUsername())
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(),
            null,
            Collections.emptyList()
        );

        String token = jwtUtil.generateToken(authentication);

        return AuthResponse.builder()
            .token(token)
            .username(user.getUsername())
            .build();
    }
}