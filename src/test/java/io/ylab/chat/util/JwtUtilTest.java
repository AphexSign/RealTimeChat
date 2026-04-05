package io.ylab.chat.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SoftAssertionsExtension.class)
@DisplayName("JWT Utility Tests")
class JwtUtilTest {

    private static final String TEST_USERNAME = "john";
    private static final String SECRET_KEY = "my-very-long-secret-key-that-is-at-least-512-bits-for-hs512-1234567890";
    private static final long EXPIRATION_MS = 3600000;
    private static final long NEGATIVE_EXPIRATION_MS = -1000;
    private static final String INVALID_TOKEN_STRING = "invalid.token.here";

    @InjectSoftAssertions
    private SoftAssertions softly;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretString",
            Base64.getEncoder().encodeToString(SECRET_KEY.getBytes()));
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", EXPIRATION_MS);
        jwtUtil.init();
    }

    @Test
    @DisplayName("Generate and validate token")
    void generateAndValidateToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null,
            List.of());
        String token = jwtUtil.generateToken(auth);
        assertThat(token).isNotEmpty();

        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo(TEST_USERNAME);

        boolean valid = jwtUtil.validateToken(token);
        softly.assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Expired token validation fails")
    void expiredToken_validationFails() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", NEGATIVE_EXPIRATION_MS);
        jwtUtil.init();
        Authentication auth = new UsernamePasswordAuthenticationToken(TEST_USERNAME, null,
            List.of());
        String token = jwtUtil.generateToken(auth);
        softly.assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Invalid token validation returns false")
    void invalidToken_throwsException() {
        softly.assertThat(jwtUtil.validateToken(INVALID_TOKEN_STRING)).isFalse();
    }
}