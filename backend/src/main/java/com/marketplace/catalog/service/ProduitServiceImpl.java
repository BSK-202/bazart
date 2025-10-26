package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduitServiceImpl implements ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitServiceImpl(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @Override
    public Optional<Produit> getProduitById(Long id) {
        return produitRepository.findById(id);
    }

    @Override
    public List<Produit> getProduitsAcceptesByCategorie(Long idCategorie) {
        // Essayez d'abord avec "accepte" (minuscules)
        List<Produit> produits = produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "accepte");

        // Si aucun résultat, essayez avec "ACCEPTE" (majuscules)
        if (produits.isEmpty()) {
            produits = produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "ACCEPTE");
        }

        // Si toujours aucun résultat, essayez avec "accepter"
        if (produits.isEmpty()) {
            produits = produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "accepter");
        }

        System.out.println("📦 Produits acceptés trouvés: " + produits.size());
        return produits;
    }

    @Override
    public List<Produit> getProduitsByCategorie(Long idCategorie) {
        return produitRepository.findByCategorieIdCategorie(idCategorie);
    }

    @Override
    public List<String> getTousLesEtats() {
        return produitRepository.findDistinctEtats(); //  CORRIGÉ : findDistinctEtats()
    }

    @Override
    public Produit saveProduit(Produit produit) {
        return produitRepository.save(produit);
    }

    @Override
    public List<Produit> getProduitsEnAttente() {
        return produitRepository.findByEtat("en_attente");
    }

    @Override
    public long countProduitsEnAttente() {
        return produitRepository.countByEtat("en_attente");
    }
}