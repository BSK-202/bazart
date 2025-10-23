package com.marketplace.unit.service;

import com.marketplace.auth.dto.AuthRequest;
import com.marketplace.auth.dto.AuthResponse;
import com.marketplace.auth.service.AuthServiceImpl;
import com.marketplace.user.entity.Client;
import com.marketplace.core.security.CustomClientDetails;
import com.marketplace.core.security.JwtTokenProvider;
import com.marketplace.user.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private ClientService clientService;
    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(authenticationManager, jwtTokenProvider, clientService);
    }

    @Test
    void shouldAuthenticateAndReturnAuthResponse() {
        AuthRequest request = AuthRequest.builder()
                .email("test@mail.com")
                .password("pass")
                .build();

        Client client = Client.builder().email("test@mail.com").build();
        CustomClientDetails details = new CustomClientDetails(client);

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(details);
        when(jwtTokenProvider.generateToken("test@mail.com")).thenReturn("token");
        when(clientService.getClientDto(client)).thenReturn(null);

        AuthResponse response = authService.authenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getClient()).isNull(); // Adapt if you return a real dto
    }

    @Test
    void shouldThrowExceptionOnInvalidCredentials() {
        AuthRequest request = AuthRequest.builder()
                .email("wrong@mail.com")
                .password("wrong")
                .build();

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Identifiants invalides"));

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Identifiants invalides");
    }
}