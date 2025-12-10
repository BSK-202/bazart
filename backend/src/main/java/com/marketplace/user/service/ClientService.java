package com.marketplace.user.service;

import com.marketplace.user.dto.ClientDto;
import com.marketplace.user.entity.Client;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface ClientService extends UserDetailsService {
    Client registerClient(ClientDto clientDto, String password);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByGoogleId(String googleId);
    ClientDto getClientDto(Client client);

    //  AJOUTER cette méthode
    Optional<Client> getClientById(Long id);
    Client updateClient(Client client);
    public void updateEmailVerified(String email) ;
    Optional<Client> findById(Long id);
}