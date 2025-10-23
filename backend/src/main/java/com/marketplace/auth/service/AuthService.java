package com.marketplace.auth.service;

import com.marketplace.auth.dto.AuthRequest;
import com.marketplace.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
    AuthResponse authenticateWithGoogle(String googleToken);
}