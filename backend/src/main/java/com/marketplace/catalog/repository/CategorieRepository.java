package com.marketplace.catalog.repository;


import com.marketplace.catalog.entity.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long> {
    List<Categorie> findByDomaine_IdDomaine(Long idDomaine);

}