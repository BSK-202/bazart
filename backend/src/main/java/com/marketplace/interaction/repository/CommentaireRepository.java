// CommentaireRepository.java - VERSION CORRIGÉE
package com.marketplace.interaction.repository;

import com.marketplace.interaction.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    // ✅ CORRECTION : Utiliser JOIN FETCH pour charger les relations
    @Query("SELECT c FROM Commentaire c JOIN FETCH c.client WHERE c.produit.idproduit = :produitId ORDER BY c.date DESC")
    List<Commentaire> findByProduitIdWithClient(@Param("produitId") Long produitId);

    // ✅ NOUVELLE MÉTHODE : Charger un commentaire avec ses relations
    @Query("SELECT c FROM Commentaire c JOIN FETCH c.client WHERE c.idcommentaire = :commentaireId")
    Optional<Commentaire> findByIdWithClient(@Param("commentaireId") Long commentaireId);

    @Query("SELECT COUNT(c) FROM Commentaire c WHERE c.produit.idproduit = :produitId")
    int countByProduitId(@Param("produitId") Long produitId);

// Dans CommentaireRepository.java - AJOUTEZ CETTE MÉTHODE AVEC @Query

    @Query("SELECT c FROM Commentaire c WHERE c.produit.idproduit = :produitId")
    List<Commentaire> findByProduitId(@Param("produitId") Long produitId);}