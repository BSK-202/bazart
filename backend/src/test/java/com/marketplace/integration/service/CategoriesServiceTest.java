package com.marketplace.integration.service;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.CategorieRepository;
import com.marketplace.catalog.service.CategorieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CategorieService.class)
@Sql(scripts = {"/schema-test.sql", "/dataCategorie-test.sql"})
class CategoriesServiceTest {

    @Autowired
    private CategorieService categorieService;



    @Test
    void testGetCategoriesByDomaine_ShouldReturnCategoriesForDomaine() {
        // Given - Les données sont chargées via data-test.sql
        Long domaineId = (Long) 1L; // Domaine Technologie

        // When
        List<Categorie> categories = categorieService.getCategoriesByDomaine(domaineId);

        // Then
        assertNotNull(categories);
        assertEquals(3, categories.size()); // 3 catégories pour le domaine Technologie

        // Vérifier la première catégorie
        Categorie premiereCategorie = categories.get(0);
        assertEquals(1L, premiereCategorie.getIdCategorie());
        assertEquals("Informatique", premiereCategorie.getNomCategorie());
        assertEquals("Catégorie pour l'informatique et les ordinateurs", premiereCategorie.getDescription());
        assertEquals("info.jpg", premiereCategorie.getImage());
        assertEquals(1L, premiereCategorie.getDomaine().getIdDomaine());

        // Vérifier que toutes les catégories appartiennent au bon domaine
        assertTrue(categories.stream().allMatch(c -> c.getDomaine().getIdDomaine().equals(domaineId)));

        // Vérifier les noms des catégories
        assertTrue(categories.stream().anyMatch(c -> "Informatique".equals(c.getNomCategorie())));
        assertTrue(categories.stream().anyMatch(c -> "Smartphones".equals(c.getNomCategorie())));
        assertTrue(categories.stream().anyMatch(c -> "Gaming".equals(c.getNomCategorie())));
    }

    @Test
    void testGetCategoriesByDomaine_WhenDomaineHasNoCategories_ShouldReturnEmptyList() {
        // Given - Domaine sans catégories (Domaine Sport - ID 4)
        Long domaineId = (Long) 4L;

        // When
        List<Categorie> categories = categorieService.getCategoriesByDomaine(domaineId);

        // Then
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
    }

    @Test
    void testGetCategoriesByDomaine_WhenDomaineDoesNotExist_ShouldReturnEmptyList() {
        // Given - Domaine inexistant
        Long domaineIdInexistant = (Long) 999L;

        // When
        List<Categorie> categories = categorieService.getCategoriesByDomaine(domaineIdInexistant);

        // Then
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
    }

    @Test
    void testGetCategorieById_ShouldReturnCategorie() {
        // Given
        Long categorieId = (Long) 1L;

        // When
        Optional<Categorie> categorie = categorieService.getCategorieById(categorieId);

        // Then
        assertTrue(categorie.isPresent());
        assertEquals("Informatique", categorie.get().getNomCategorie());
        assertEquals(1L, categorie.get().getDomaine().getIdDomaine());
    }

    @Test
    void testGetCategorieById_WhenCategorieDoesNotExist_ShouldReturnEmpty() {
        // Given
        Long categorieIdInexistant = (Long) 999L;

        // When
        Optional<Categorie> categorie = categorieService.getCategorieById(categorieIdInexistant);

        // Then
        assertFalse(categorie.isPresent());
    }

    @Test
    void testGetCategoriesByDomaine_WhenNoData_ShouldReturnEmptyList() {
        // Given - Seul le schéma est créé, pas de données
        Long domaineId = (Long) 4L;

        // When
        List<Categorie> categories = categorieService.getCategoriesByDomaine(domaineId);

        // Then
        assertNotNull(categories);
        assertTrue(categories.isEmpty());
    }
}