package com.marketplace.integration.controller;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.catalog.repository.CategorieRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
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
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Categorie testCategorie;
    private Client testVendeur;

    @BeforeEach
    void setUp() {
        // ✅ SIMPLE - juste récupérer les données de H2
        testCategorie = categorieRepository.findById(Long.valueOf(1L))
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée dans H2"));
        testVendeur = clientRepository.findById(Long.valueOf(1L))
                .orElseThrow(() -> new RuntimeException("Client non trouvé dans H2"));

        // Vérification des données H2
        assertThat(categorieRepository.findAll()).hasSize(3);
        assertThat(clientRepository.findAll()).hasSize(2);
        assertThat(produitRepository.findAll()).hasSize(3); // Les 3 produits de data-test.sql
    }

    @Test
    void getProduitById_ShouldReturnProduit_WhenProduitExists() throws Exception {
        // Given - Utiliser les produits existants de H2
        Produit produit = produitRepository.findById(Long.valueOf(1L))
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
        Long produitId = produit.getIdproduit();

        // When & Then
        mockMvc.perform(get("/api/produits/{id}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(produitId.intValue())))
                .andExpect(jsonPath("$.nom", is(produit.getNom())))
                .andExpect(jsonPath("$.description", is(produit.getDescription())))
                .andExpect(jsonPath("$.prixDebut", is(produit.getPrixDebut())))
                .andExpect(jsonPath("$.prixFin", is(produit.getPrixFin())))
                .andExpect(jsonPath("$.etat", is(produit.getEtat())));
    }

    @Test
    void getProduitById_ShouldReturn404_WhenProduitNotFound() throws Exception {
        // Given
        Long nonExistentId = (Long) (Long) 999L;

        // When & Then
        mockMvc.perform(get("/api/produits/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProduitsAcceptesByCategorie_ShouldReturnOnlyAcceptedProduits() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/produits/categorie/{idCategorie}/acceptes", testCategorie.getIdCategorie())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].etat", everyItem(is("accepte"))));
    }

    @Test
    void getAllProduitsByCategorie_ShouldReturnAllProduits() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/produits/categorie/{idCategorie}/all", testCategorie.getIdCategorie())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].categorieNom", everyItem(is("Informatique"))));
    }

    @Test
    void createProduit_ShouldCreateNewProduit() throws Exception {
        // Given
        String produitJson = """
            {
                "nom": "Nouveau Produit Test H2",
                "description": "Description du nouveau produit créé via test H2",
                "prixDebut": 500.0,
                "prixFin": 700.0,
                "aExpertise": false,
                "categorieId": %d,
                "vendeurId": %d
            }
            """.formatted(testCategorie.getIdCategorie(), testVendeur.getId());

        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "image1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "contenu image test".getBytes()
        );

        MockMultipartFile produitRequest = new MockMultipartFile(
                "produit",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                produitJson.getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/produits")
                        .file(produitRequest)
                        .file(image1)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom", is("Nouveau Produit Test H2")))
                .andExpect(jsonPath("$.etat", is("en_attente")))
                .andExpect(jsonPath("$.prixDebut", is(500.0)))
                .andExpect(jsonPath("$.prixFin", is(700.0)));
    }

    @Test
    void getProduitsAcceptesByCategorie_ShouldReturnEmptyList_WhenNoAcceptedProduits() throws Exception {
        // Given - Utiliser une catégorie sans produits acceptés
        Categorie categorieSante = categorieRepository.findById(Long.valueOf(3L))
                .orElseThrow(() -> new RuntimeException("Catégorie santé non trouvée"));

        // When & Then
        mockMvc.perform(get("/api/produits/categorie/{idCategorie}/acceptes", categorieSante.getIdCategorie())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getImage_ShouldReturn404_WhenImageNotFound() throws Exception {
        // Given
        Long produitId = (Long) 999L;
        String fileName = "nonexistent.jpg";

        // When & Then
        mockMvc.perform(get("/api/produits/images/{produitId}/{fileName}", produitId, fileName))
                .andExpect(status().isNotFound());
    }

    @Test
    void database_ShouldContainCorrectTestDataFromH2() {
        // Test direct sur le repository avec données H2
        List<Categorie> categories = categorieRepository.findAll();
        List<Client> clients = clientRepository.findAll();
        List<Produit> produits = produitRepository.findAll();

        assertThat(categories).hasSize(3);
        assertThat(clients).hasSize(2);
        assertThat(produits).hasSize(3);

        // Vérifier un produit spécifique
        Produit laptop = produitRepository.findById(Long.valueOf(1L))
                .orElseThrow(() -> new RuntimeException("Laptop non trouvé"));

        assertThat(laptop.getNom()).isEqualTo("Laptop Gaming");
        assertThat(laptop.getEtat()).isEqualTo("accepte");
    }
}