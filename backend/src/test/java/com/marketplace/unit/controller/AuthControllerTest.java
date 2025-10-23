package com.marketplace.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.auth.controller.AuthController;
import com.marketplace.auth.dto.AuthRequest;
import com.marketplace.auth.dto.AuthResponse;
import com.marketplace.user.dto.ClientDto;
import com.marketplace.user.entity.Client;
import com.marketplace.auth.service.AuthService;
import com.marketplace.user.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
// Alternative 1: Configuration class avec @MockitoBean
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Nouvelle annotation recommandée
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ClientService clientService;

    private ObjectMapper objectMapper;
    private AuthRequest authRequest;
    private AuthResponse authResponse;
    private ClientDto clientDto;
    private Client client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        authRequest = AuthRequest.builder()
                .email("test@mail.com")
                .password("123456")
                .nom("Doe")
                .prenom("John")
                .tel("0102030405")
                .pays("FR")
                .ville("Paris")
                .photoprofil("profile.jpg")
                .build();

        clientDto = ClientDto.builder()
                .idclient(1L)
                .email("test@mail.com")
                .nom("Doe")
                .prenom("John")
                .tel("0102030405")
                .pays("FR")
                .ville("Paris")
                .photoprofil("profile.jpg")
                .enabled(true)
                .build();

        client = Client.builder()
                .idclient(1L)
                .email("test@mail.com")
                .nom("Doe")
                .prenom("John")
                .tel("0102030405")
                .pays("FR")
                .ville("Paris")
                .photoprofil("profile.jpg")
                .enabled(true)
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .client(clientDto)
                .build();
    }

    @Test
    void shouldRegisterClientSuccessfully() throws Exception {
        when(clientService.registerClient(any(ClientDto.class), eq("123456"))).thenReturn(client);
        when(clientService.getClientDto(client)).thenReturn(clientDto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@mail.com"))
                .andExpect(jsonPath("$.nom").value("Doe"))
                .andExpect(jsonPath("$.prenom").value("John"));
    }

    @Test
    void shouldAuthenticateSuccessfully() throws Exception {
        when(authService.authenticate(any(AuthRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.client.email").value("test@mail.com"));
    }
}