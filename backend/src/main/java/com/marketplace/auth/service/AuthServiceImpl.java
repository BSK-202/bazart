package com.marketplace.auth.service;

import com.marketplace.auth.dto.AuthRequest;
import com.marketplace.auth.dto.AuthResponse;
import com.marketplace.user.entity.Client;
import com.marketplace.core.security.CustomClientDetails;
import com.marketplace.core.security.JwtTokenProvider;
import com.marketplace.user.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ClientService clientService;

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            Client client = ((CustomClientDetails) authentication.getPrincipal()).getClient();
            String token = jwtTokenProvider.generateToken(client.getEmail());
            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .client(clientService.getClientDto(client))
                    .build();
        } catch (AuthenticationException e) {
            throw new RuntimeException("Identifiants invalides");
        }
    }

    @Override
    public AuthResponse authenticateWithGoogle(String googleToken) {
        throw new UnsupportedOperationException("Google login non implémenté");
    }
}