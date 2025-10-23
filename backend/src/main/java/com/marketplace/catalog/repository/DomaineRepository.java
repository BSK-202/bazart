package com.marketplace.catalog.repository;


import com.marketplace.catalog.entity.Domaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomaineRepository extends JpaRepository<Domaine, Long> {
    // Ici, pas besoin d’écrire de méthode, JpaRepository fournit déjà findAll()
}