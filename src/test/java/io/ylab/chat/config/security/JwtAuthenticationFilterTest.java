package io.ylab.chat.config.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ylab.chat.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VALID_TOKEN = "valid.token";
    private static final String INVALID_TOKEN = "invalid";
    private static final String USERNAME_JOHN = "john";

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    @DisplayName("Valid token - sets authentication and continues chain")
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + VALID_TOKEN);
        when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(USERNAME_JOHN);
        when(jwtUtil.validateToken(VALID_TOKEN, USERNAME_JOHN)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        softly.assertThat(auth).isNotNull();
        softly.assertThat(auth.getName()).isEqualTo(USERNAME_JOHN);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid token - does not set authentication and continues chain")
    void doFilterInternal_invalidToken_doesNotSetAuthentication() throws Exception {
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + INVALID_TOKEN);
        when(jwtUtil.extractUsername(INVALID_TOKEN)).thenThrow(new RuntimeException());

        filter.doFilterInternal(request, response, chain);

        softly.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("No token - continues chain without authentication")
    void doFilterInternal_noToken_continuesChain() throws Exception {
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        softly.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}