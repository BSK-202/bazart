package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Produit;
import java.util.List;
import java.util.Optional;

public interface ProduitService {
    Optional<Produit> getProduitById(Long id);
    List<Produit> getProduitsAcceptesByCategorie(Long idCategorie);
    List<Produit> getProduitsByCategorie(Long idCategorie); //  CORRIGÉ
    List<String> getTousLesEtats(); //  CORRIGÉ (nom correct)
    Produit saveProduit(Produit produit);
    List<Produit> getProduitsEnAttente();
    long countProduitsEnAttente();
}
