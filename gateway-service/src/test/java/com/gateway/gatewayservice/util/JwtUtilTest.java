package com.gateway.gatewayservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
        "MySecretKey123456789012345678901234567890Ab";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    private String createToken(String userId, String username, long expiryMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("role", "USER");
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Valid token should pass validation")
    void validToken_shouldReturnTrue() {
        String token = createToken("1", "testuser", 900_000);
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void expiredToken_shouldReturnFalse() {
        String token = createToken("1", "testuser", -1000); // already expired
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Invalid token string should fail validation")
    void invalidToken_shouldReturnFalse() {
        assertFalse(jwtUtil.isTokenValid("this.is.invalid"));
    }

    @Test
    @DisplayName("Should extract correct userId from token")
    void shouldExtractUserId() {
        String token = createToken("42", "testuser", 900_000);
        assertEquals("42", jwtUtil.extractUserId(token));
    }

    @Test
    @DisplayName("Should extract correct username from token")
    void shouldExtractUsername() {
        String token = createToken("1", "shekhar", 900_000);
        assertEquals("shekhar", jwtUtil.extractUsername(token));
    }

    @Test
    @DisplayName("Tampered token should fail validation")
    void tamperedToken_shouldReturnFalse() {
        String token = createToken("1", "testuser", 900_000);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.isTokenValid(tampered));
    }
}