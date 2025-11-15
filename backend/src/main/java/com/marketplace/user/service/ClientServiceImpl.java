package com.marketplace.user.service;

import com.marketplace.user.dto.ClientDto;
import com.marketplace.user.entity.Client;
import com.marketplace.auth.entity.Role;
import com.marketplace.user.repository.ClientRepository;
import com.marketplace.auth.repository.RoleRepository;
import com.marketplace.core.security.CustomClientDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Client client = clientRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Client non trouvé: " + username));
        return new CustomClientDetails(client);
    }

    @Override
    public Client registerClient(ClientDto clientDto, String password) {
        if (clientRepository.existsByEmail(clientDto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("USER");
                    System.out.println(" Création automatique du rôle USER");
                    return roleRepository.save(newRole);
                });

        Client client = Client.builder()
                .nom(clientDto.getNom())
                .prenom(clientDto.getPrenom())
                .email(clientDto.getEmail())
                .motdepasse(passwordEncoder.encode(password))
                .tel(clientDto.getTel())
                .pays(clientDto.getPays())
                .ville(clientDto.getVille())
                .photoprofil(clientDto.getPhotoprofil())
                .roles(Collections.singleton(userRole))
                .enabled(true)
                .build();

        Client savedClient = clientRepository.save(client);
        System.out.println(" Client créé avec succès: " + savedClient.getEmail());
        return savedClient;
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    @Override
    public Optional<Client> findByGoogleId(String googleId) {
        return clientRepository.findByGoogleId(googleId);
    }

    @Override
    public ClientDto getClientDto(Client client) {
        Set<String> roles = client.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return ClientDto.builder()
                .idclient(client.getIdclient())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .email(client.getEmail())
                .tel(client.getTel())
                .pays(client.getPays())
                .ville(client.getVille())
                .photoprofil(client.getPhotoprofil())
                .roles(roles)
                .enabled(client.isEnabled())
                .build();
    }
    @Override
    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }
    @Override
    public Client updateClient(Client client) {
        if (!clientRepository.existsById(client.getIdclient())) {
            throw new RuntimeException("Client non trouvé");
        }
        return clientRepository.save(client);
    }

    public void updateEmailVerified(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        client.setEmailVerified(true);
        clientRepository.save(client);
    }

}