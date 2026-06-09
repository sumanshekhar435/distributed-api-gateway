package com.auth.authservice.service;

import com.auth.authservice.model.dto.request.LoginRequest;
import com.auth.authservice.model.dto.request.RefreshTokenRequest;
import com.auth.authservice.model.dto.request.RegisterRequest;
import com.auth.authservice.model.dto.response.AuthResponse;

public interface AuthService {
    String register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
}