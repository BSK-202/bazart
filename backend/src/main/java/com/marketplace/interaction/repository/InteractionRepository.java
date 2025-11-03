package com.marketplace.interaction.repository;

import com.marketplace.interaction.entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // Vérifier si un client a déjà liké un produit
    @Query("SELECT i FROM Interaction i WHERE i.produit.idproduit = :produitId AND i.client.idclient = :clientId")
    Optional<Interaction> findByProduitIdAndClientId(@Param("produitId") Long produitId, @Param("clientId") Long clientId);

    // Compter le nombre d'interactions pour un produit
    @Query("SELECT COUNT(i) FROM Interaction i WHERE i.produit.idproduit = :produitId")
    int countByProduitId(@Param("produitId") Long produitId);

    // ✅ CORRECTION: Utiliser une méthode de suppression standard
    @Transactional
    @Modifying
    @Query("DELETE FROM Interaction i WHERE i.produit.idproduit = :produitId AND i.client.idclient = :clientId")
    int deleteByProduitIdAndClientId(@Param("produitId") Long produitId, @Param("clientId") Long clientId);
    @Query("SELECT i FROM Interaction i WHERE i.client.idclient = :clientId ORDER BY i.date DESC")
    List<Interaction> findByClientId(@Param("clientId") Long clientId);
}