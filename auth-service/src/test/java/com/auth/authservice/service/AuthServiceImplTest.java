package com.auth.authservice.service;

import com.auth.authservice.model.dto.request.LoginRequest;
import com.auth.authservice.model.dto.request.RegisterRequest;
import com.auth.authservice.model.dto.response.AuthResponse;
import com.auth.authservice.model.entity.RefreshToken;
import com.auth.authservice.model.entity.User;
import com.auth.authservice.repository.RefreshTokenRepository;
import com.auth.authservice.repository.UserRepository;
import com.auth.authservice.service.impl.AuthServiceImpl;
import com.auth.authservice.util.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Register - new user should be saved successfully")
    void register_newUser_shouldSaveSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "Test@123", "test@test.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(new User());

        String result = authService.register(request);

        assertTrue(result.contains("testuser"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register - duplicate username should throw exception")
    void register_duplicateUsername_shouldThrowException() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "Test@123", "test@test.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - valid credentials should return tokens")
    void login_validCredentials_shouldReturnTokens() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("hashed-password")
                .role("USER")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("uuid-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test@123", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token-here");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);

        AuthResponse response = authService.login(
                new LoginRequest("testuser", "Test@123"));

        assertNotNull(response);
        assertEquals("access-token-here", response.getAccessToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("Login - wrong password should throw exception")
    void login_wrongPassword_shouldThrowException() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("hashed-password")
                .role("USER")
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.login(
                        new LoginRequest("testuser", "wrongpassword")));
    }

    @Test
    @DisplayName("Login - user not found should throw exception")
    void login_userNotFound_shouldThrowException() {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.login(
                        new LoginRequest("unknown", "Test@123")));
    }
}