package com.marketplace.integration.controller;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.CategorieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(scripts = {"/schema-test.sql", "/dataCategorieIntg-test.sql"})
class CategorieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategorieRepository categorieRepository;

    @BeforeEach
    void setUp() {
        // Vérification que les données de test H2 sont bien chargées
        List<Categorie> categories = categorieRepository.findAll();
        assertThat(categories).hasSize(4); // 4 catégories au total
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnCategoriesForDomaine() throws Exception {
        // Given - Domaine Technologie (id=1) depuis data-test.sql
        Long domaineId = (Long) 1L;

        // When & Then
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // 2 catégories pour le domaine Technologie
                .andExpect(jsonPath("$[0].nomCategorie", is("Informatique")))
                .andExpect(jsonPath("$[0].description", is("Ordinateurs et accessoires")))
                .andExpect(jsonPath("$[0].image", is("info.jpg")))
                .andExpect(jsonPath("$[1].nomCategorie", is("Téléphonie")))
                .andExpect(jsonPath("$[1].description", is("Smartphones et tablettes")))
                .andExpect(jsonPath("$[1].image", is("phone.jpg")));
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnEmptyList_WhenDomaineHasNoCategories() throws Exception {
        // Given - Domaine sans catégories (id=3 - Éducation) depuis data-test.sql
        Long domaineId = (Long) 3L;

        // When & Then
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCategorieById_ShouldReturnCategorie() throws Exception {
        // Given - Catégorie existante (id=1) depuis data-test.sql
        Long categorieId = (Long) 1L;

        // When & Then
        mockMvc.perform(get("/api/categories/{id}", categorieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCategorie", is(1)))
                .andExpect(jsonPath("$.nomCategorie", is("Informatique")))
                .andExpect(jsonPath("$.description", is("Ordinateurs et accessoires")))
                .andExpect(jsonPath("$.image", is("info.jpg")));
    }

    @Test
    void getCategorieById_ShouldReturn404_WhenCategorieNotFound() throws Exception {
        // Given - Catégorie inexistante
        Long categorieId = (Long) 999L;

        // When & Then
        mockMvc.perform(get("/api/categories/{id}", categorieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategorieImage_ShouldReturn404_WhenImageNotFound() throws Exception {
        // Given
        String nonExistentImage = "nonexistent.jpg";

        // When & Then
        mockMvc.perform(get("/api/categories/images/" + nonExistentImage))
                .andExpect(status().isNotFound());
    }

    @Test
    void database_ShouldContainExactlyFourCategoriesFromH2() {
        // Test direct sur le repository avec données H2
        List<Categorie> categories = categorieRepository.findAll();
        assertThat(categories).hasSize(4);

        // Vérifier la première catégorie (Informatique)
        Categorie informatique = categories.stream()
                .filter(c -> c.getIdCategorie() == 1L)
                .findFirst()
                .orElseThrow();

        assertThat(informatique.getNomCategorie()).isEqualTo("Informatique");
        assertThat(informatique.getDescription()).isEqualTo("Ordinateurs et accessoires");
        assertThat(informatique.getImage()).isEqualTo("info.jpg");

        // Vérifier la deuxième catégorie (Téléphonie)
        Categorie telephonie = categories.stream()
                .filter(c -> c.getIdCategorie() == 2L)
                .findFirst()
                .orElseThrow();

        assertThat(telephonie.getNomCategorie()).isEqualTo("Téléphonie");
        assertThat(telephonie.getDescription()).isEqualTo("Smartphones et tablettes");
        assertThat(telephonie.getImage()).isEqualTo("phone.jpg");

        // Vérifier la troisième catégorie (Médecine)
        Categorie medecine = categories.stream()
                .filter(c -> c.getIdCategorie() == 3L)
                .findFirst()
                .orElseThrow();

        assertThat(medecine.getNomCategorie()).isEqualTo("Médecine");
        assertThat(medecine.getDescription()).isEqualTo("Équipements médicaux");
        assertThat(medecine.getImage()).isEqualTo("medecine.jpg");

        // Vérifier la quatrième catégorie (Football)
        Categorie football = categories.stream()
                .filter(c -> c.getIdCategorie() == 4L)
                .findFirst()
                .orElseThrow();

        assertThat(football.getNomCategorie()).isEqualTo("Football");
        assertThat(football.getDescription()).isEqualTo("Équipements de football");
        assertThat(football.getImage()).isEqualTo("football.jpg");
    }

    @Test
    void getCategoriesByDomaine_ShouldHaveCorrectCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/categories/domaine/1")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000")); // Corrigé
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnJsonContentType() throws Exception {
        mockMvc.perform(get("/api/categories/domaine/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnCorrectDomainRelation() throws Exception {
        // Given - Domaine Santé (id=2) qui a 1 catégorie (Médecine)
        Long domaineId = (Long) 2L;

        // When & Then
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomCategorie", is("Médecine")))
                .andExpect(jsonPath("$[0].description", is("Équipements médicaux")))
                .andExpect(jsonPath("$[0].image", is("medecine.jpg")));
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnCategoriesForDomaineSport() throws Exception {
        // Given - Domaine Sport (id=4) qui a 1 catégorie (Football)
        Long domaineId = (Long) 4L;

        // When & Then
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomCategorie", is("Football")))
                .andExpect(jsonPath("$[0].description", is("Équipements de football")))
                .andExpect(jsonPath("$[0].image", is("football.jpg")));
    }

    @Test
    void getAllCategories_ShouldReturnAllCategories() throws Exception {
        // When & Then - Vérifier qu'on peut récupérer toutes les catégories via le repository
        List<Categorie> categories = categorieRepository.findAll();

        assertThat(categories).hasSize(4);
        assertThat(categories)
                .extracting(Categorie::getNomCategorie)
                .containsExactlyInAnyOrder("Informatique", "Téléphonie", "Médecine", "Football");
    }

    @Test
    void getCategorieImage_ShouldReturn400_WhenImagePathIsInvalid() throws Exception {
        // Given - Un nom de fichier avec des caractères spéciaux
        String problematicImage = "../invalid-image.jpg";

        // When & Then - Accepte 400 (Bad Request) de Spring Security
        mockMvc.perform(get("/api/categories/images/" + problematicImage))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Accepte 400 pour les URLs non normalisées
                    assertThat(status).isEqualTo(400);
                });
    }
}