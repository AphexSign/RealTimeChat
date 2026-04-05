package io.ylab.chat.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * Utility class for generating, parsing, and validating JSON Web Tokens (JWT).
 */
@Component
public class JwtUtil {

    private static final String AUTHORITIES_CLAIM = "authorities";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_VALUE = "JWT";

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey key;

    /**
     * Initializes the HMAC signing key from the configured secret.
     *
     * @throws IllegalStateException if the secret is empty, null, or shorter than 32 bytes
     */
    @PostConstruct
    public void init() {
        if (secretString == null || secretString.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must not be empty");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretString);
        } catch (IllegalArgumentException e) {
            keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 256 bits (32 bytes) for HS256, 512 bits for HS512");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for the given authentication object.
     *
     * @param authentication the Spring Security authentication containing username and authorities
     * @return a compact, signed JWT string
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return Jwts.builder()
            .subject(username)
            .claim(AUTHORITIES_CLAIM, authorities)
            .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_VALUE)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS512)
            .compact();
    }

    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token the JWT string
     * @return the username, or {@code null} if the token is invalid (caller should use
     * {@link #validateToken(String)} first)
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT string
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Checks whether the token has expired.
     *
     * @param token the JWT string
     * @return {@code true} if the token is expired, {@code false} otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the list of authority strings from the token's {@code authorities} claim.
     *
     * @param token the JWT string
     * @return a list of authorities (never {@code null}); empty list if claim is missing
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.getOrDefault(AUTHORITIES_CLAIM, Collections.emptyList());
    }

    /**
     * Validates the token signature and expiration.
     *
     * @param token the JWT string
     * @return {@code true} if the token is well-formed, signed correctly, and not expired
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Validates the token against an expected username.
     *
     * @param token            the JWT string
     * @param expectedUsername the username that should match the token's subject
     * @return {@code true} if the token is valid and the subject matches the expected username
     */
    public boolean validateToken(String token, String expectedUsername) {
        try {
            String username = extractUsername(token);
            return username.equals(expectedUsername) && validateToken(token);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Parses and verifies the JWT token, returning its claims payload.
     *
     * @param token the JWT string
     * @return the claims
     * @throws JwtException if the token is malformed, expired, or has an invalid signature
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}