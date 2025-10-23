package com.marketplace.unit.service;

import com.marketplace.user.dto.ClientDto;
import com.marketplace.user.entity.Client;
import com.marketplace.auth.entity.Role;
import com.marketplace.user.repository.ClientRepository;
import com.marketplace.auth.repository.RoleRepository;
import com.marketplace.user.service.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientService = new ClientServiceImpl(clientRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterNewClient() {
        ClientDto clientDto = ClientDto.builder()
                .email("test@mail.com")
                .nom("Doe")
                .prenom("John")
                .build();

        Role userRole = new Role();
        userRole.setName("USER");

        when(clientRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client client = clientService.registerClient(clientDto, "pass");

        assertThat(client.getEmail()).isEqualTo("test@mail.com");
        assertThat(client.getMotdepasse()).isEqualTo("hashed");
        assertThat(client.getRoles()).contains(userRole);
        assertThat(client.isEnabled()).isTrue();
    }

    @Test
    void shouldThrowIfEmailExists() {
        ClientDto clientDto = ClientDto.builder().email("test@mail.com").build();
        when(clientRepository.existsByEmail("test@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clientService.registerClient(clientDto, "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email déjà utilisé");
    }
}