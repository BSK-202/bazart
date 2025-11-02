package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.CategorieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieService(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    public List<Categorie> getCategoriesByDomaine(Long idDomaine) {
        return categorieRepository.findByDomaine_IdDomaine(idDomaine);
    }
    public Optional<Categorie> getCategorieById(Long id) {
        return categorieRepository.findById(id);
    }

    // CategorieService.java

    public Categorie addCategorie(Categorie categorie) {
        return categorieRepository.save(categorie);
    }

    public Categorie updateCategorie(Long id, Categorie categorieDetails) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

        categorie.setNomCategorie(categorieDetails.getNomCategorie());
        categorie.setDescription(categorieDetails.getDescription());

        // Ne met à jour l'image que si elle est fournie dans categorieDetails
        if (categorieDetails.getImage() != null) {
            categorie.setImage(categorieDetails.getImage());
        }

        return categorieRepository.save(categorie);
    }

    public boolean deleteCategorie(Long id) {
        if (categorieRepository.existsById(id)) {
            categorieRepository.deleteById(id);
            return true;
        }
        return false;
    }
    public long getCategoriesCountByDomaine(Long idDomaine) {
        return categorieRepository.findByDomaine_IdDomaine(idDomaine).size();
    }


}