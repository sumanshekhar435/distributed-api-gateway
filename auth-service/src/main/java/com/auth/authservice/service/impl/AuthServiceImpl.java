package com.auth.authservice.service.impl;

import com.auth.authservice.model.dto.request.LoginRequest;
import com.auth.authservice.model.dto.request.RefreshTokenRequest;
import com.auth.authservice.model.dto.request.RegisterRequest;
import com.auth.authservice.model.dto.response.AuthResponse;
import com.auth.authservice.model.entity.RefreshToken;
import com.auth.authservice.model.entity.User;
import com.auth.authservice.repository.RefreshTokenRepository;
import com.auth.authservice.repository.UserRepository;
import com.auth.authservice.service.AuthService;
import com.auth.authservice.util.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("USER")
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", request.getUsername());
        return "User registered successfully: " + request.getUsername();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for user: {}", request.getUsername());
            throw new RuntimeException("Invalid credentials");
        }

        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User logged in successfully: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiry))
                .build();
        return refreshTokenRepository.save(token);
    }
}