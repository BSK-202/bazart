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
}