package io.ylab.chat.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ylab.chat.dto.AuthResponse;
import io.ylab.chat.dto.LoginRequest;
import io.ylab.chat.dto.RegisterRequest;
import io.ylab.chat.entity.UserEntity;
import io.ylab.chat.repository.UserRepository;
import io.ylab.chat.util.JwtUtil;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("UserService Implementation Tests")
class UserServiceImplTest {

    private static final String USERNAME_JOHN = "john";
    private static final String PASSWORD_RAW = "pass";
    private static final String PASSWORD_ENCODED = "encoded";
    private static final String JWT_TOKEN = "jwt.token";
    private static final String JWT_TOKEN_ALT = "jwt";
    private static final String EXISTING_USER_MESSAGE = "Username already exists";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    @DisplayName("Register new user - success")
    void register_newUser_success() {
        RegisterRequest request = new RegisterRequest(USERNAME_JOHN, PASSWORD_RAW);
        when(userRepository.existsByUsername(USERNAME_JOHN)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD_RAW)).thenReturn(PASSWORD_ENCODED);
        when(jwtUtil.generateToken(any(Authentication.class))).thenReturn(JWT_TOKEN);

        AuthResponse response = userService.register(request);

        softly.assertThat(response.getUsername()).isEqualTo(USERNAME_JOHN);
        softly.assertThat(response.getToken()).isEqualTo(JWT_TOKEN);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Register existing user - throws exception")
    void register_existingUser_throwsException() {
        when(userRepository.existsByUsername(USERNAME_JOHN)).thenReturn(true);
        RegisterRequest request = new RegisterRequest(USERNAME_JOHN, PASSWORD_RAW);

        softly.assertThatThrownBy(() -> userService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(EXISTING_USER_MESSAGE);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login with valid credentials - success")
    void login_validCredentials_success() {
        UserEntity user = UserEntity.builder()
            .username(USERNAME_JOHN)
            .passwordHash(PASSWORD_ENCODED)
            .build();
        when(userRepository.findByUsername(USERNAME_JOHN)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD_RAW, PASSWORD_ENCODED)).thenReturn(true);
        when(jwtUtil.generateToken(any(Authentication.class))).thenReturn(JWT_TOKEN_ALT);

        AuthResponse response = userService.login(new LoginRequest(USERNAME_JOHN, PASSWORD_RAW));

        softly.assertThat(response.getToken()).isEqualTo(JWT_TOKEN_ALT);
        softly.assertThat(response.getUsername()).isEqualTo(USERNAME_JOHN);
    }

    @Test
    @DisplayName("Login with invalid password - throws exception")
    void login_invalidPassword_throwsException() {
        UserEntity user = UserEntity.builder()
            .username(USERNAME_JOHN)
            .passwordHash(PASSWORD_ENCODED)
            .build();
        when(userRepository.findByUsername(USERNAME_JOHN)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", PASSWORD_ENCODED)).thenReturn(false);

        softly.assertThatThrownBy(() -> userService.login(new LoginRequest(USERNAME_JOHN, "wrong")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(INVALID_CREDENTIALS_MESSAGE);
    }
}