package com.marketplace.auth.controller;

import com.marketplace.auth.dto.AuthRequest;
import com.marketplace.auth.dto.AuthResponse;
import com.marketplace.user.dto.ClientDto;
import com.marketplace.user.entity.Client;
import com.marketplace.auth.service.AuthService;
import com.marketplace.user.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ClientService clientService;

    @PostMapping("/register")
    public ResponseEntity<ClientDto> register(@RequestBody AuthRequest request) {
        ClientDto clientDto = ClientDto.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .tel(request.getTel())
                .pays(request.getPays())
                .ville(request.getVille())
                .photoprofil(request.getPhotoprofil())
                .build();

        Client client = clientService.registerClient(clientDto, request.getPassword());
        return ResponseEntity.ok(clientService.getClientDto(client));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}