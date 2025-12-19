package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Produit;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProduitService {
    Optional<Produit> getProduitById(Long id);
    List<Produit> getProduitsAcceptesByCategorie(Long idCategorie);
    List<Produit> getProduitsByCategorie(Long idCategorie); //  CORRIGÉ
    List<String> getTousLesEtats(); //  CORRIGÉ (nom correct)
    Produit saveProduit(Produit produit);
    List<Produit> getProduitsEnAttente();
    long countProduitsEnAttente();
    List<Produit> getProduitsByVendeur(Long vendeurId);
    // 🆕 NOUVELLES MÉTHODES POUR LA GESTION DES IMAGES
    void deleteProduitImages(Produit produit, List<String> imageUrlsToDelete);
    void addProduitImages(Produit produit, List<MultipartFile> newImages, Path produitFolderPath) throws IOException;
    List<Produit> getProduitsByEtat(String enEnchere);
    // Dans ProduitService.java, ajouter cette signature de méthode
    List<Produit> getProduitsByAcheteur(Long acheteurId);
    // Dans ProduitService.java
    Set<Long> getUsersWhoInteractedWithProduct(Long produitId);
    List<Produit> getProduitsGagnesByAcheteurId(Long acheteurId);
    List<Produit> getProduitsByVendeurId(Long vendeurId);
}
