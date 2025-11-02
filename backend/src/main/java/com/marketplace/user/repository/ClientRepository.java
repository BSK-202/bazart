package com.marketplace.user.repository;

import com.marketplace.user.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
	Optional<Client> findByEmail(String email);
	Optional<Client> findByGoogleId(String googleId);
	boolean existsByEmail(String email);
	
	// Récupère uniquement l'email d'un utilisateur par son id (optimisé)
    @Query("SELECT c.email FROM Client c WHERE c.id = :id")
    Optional<String> findEmailById(@Param("id") Long id);
}