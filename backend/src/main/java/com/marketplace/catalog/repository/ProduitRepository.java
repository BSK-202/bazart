package com.marketplace.catalog.repository;

import com.marketplace.catalog.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {


    List<Produit> findByCategorieIdCategorieAndEtat(Long idCategorie, String etat);


    List<Produit> findByCategorieIdCategorie(Long idCategorie);


    List<Produit> findByEtat(String etat);

    List<Produit> findByVendeurIdclient(Long vendeurId);

    @Query("SELECT DISTINCT p.etat FROM Produit p WHERE p.etat IS NOT NULL")
    List<String> findDistinctEtats();
    //List<Produit> findByEtat(String etat);
    long countByEtat(String etat);
    // Dans ProduitRepository.java, ajouter cette méthode
    List<Produit> findByAcheteurIdclient(Long acheteurId);

    List<Produit> findByAcheteurIdAndEtat(Long acheteurId, String enchereTermine);

    List<Produit> findByVendeurId(Long vendeurId);
}