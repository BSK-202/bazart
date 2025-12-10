package com.marketplace.Enchere.repository;




import aj.org.objectweb.asm.commons.Remapper;
import com.marketplace.Enchere.entity.Enchere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnchereRepository extends JpaRepository<Enchere, Long> {

    // Trouver toutes les enchères d'un produit triées par montant décroissant
    List<Enchere> findByProduitIdproduitOrderByMontantDesc(Long produitId);

    // Trouver la dernière enchère (la plus haute) pour un produit
    @Query("SELECT e FROM Enchere e WHERE e.produit.idproduit = :produitId ORDER BY e.montant DESC, e.dateEnchere ASC LIMIT 1")
    Optional<Enchere> findTopByProduitIdOrderByMontantDesc(@Param("produitId") Long produitId);

    // Trouver toutes les enchères d'un utilisateur
    List<Enchere> findByEncherisseurIdclient(Long clientId);

    // Vérifier si un utilisateur a déjà enchéri sur un produit
    boolean existsByEncherisseurIdclientAndProduitIdproduit(Long clientId, Long produitId);

    // Compter le nombre d'enchères pour un produit
    Long countByProduitIdproduit(Long produitId);

    // Trouver le montant maximum pour un produit
    @Query("SELECT MAX(e.montant) FROM Enchere e WHERE e.produit.idproduit = :produitId")
    Optional<Double> findMaxMontantByProduitId(@Param("produitId") Long produitId);


}