package com.marketplace.integration.controller;

import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.repository.CommentaireRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")

@Sql(scripts = {"/schema-test.sql", "/dataCommentaireIntgTest.sql"})
class CommentaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long clientId1;
    private Long clientId2;
    private Long clientId3;
    private Long produitId1;
    private Long produitId2;
    private Long produitId3;
    private Long produitId4;

    @BeforeEach
    void setUp() {
        // Récupérer les clients et produits depuis la base de test H2
        List<Client> clients = clientRepository.findAll();
        List<Produit> produits = produitRepository.findAll();

        assertThat(clients).isNotEmpty();
        assertThat(produits).isNotEmpty();

        clientId1 = clients.get(0).getIdclient();
        clientId2 = clients.get(1).getIdclient();
        clientId3 = clients.get(2).getIdclient();
        produitId1 = produits.get(0).getIdproduit();
        produitId2 = produits.get(1).getIdproduit();
        produitId3 = produits.get(2).getIdproduit();
        produitId4 = produits.get(3).getIdproduit();
    }

    @Test
    void getCommentairesByProduit_ShouldReturnComments_WhenProductHasComments() throws Exception {
        // Given - Produit 3 a 3 commentaires
        Long produitId = produitId3;

        // When & Then
        mockMvc.perform(get("/api/commentaires/produit/{produitId}", produitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].contenu", notNullValue()))
                .andExpect(jsonPath("$[0].client", notNullValue()))
                .andExpect(jsonPath("$[0].client.idclient", notNullValue()))
                .andExpect(jsonPath("$[0].client.nom", notNullValue()))
                .andExpect(jsonPath("$[0].client.prenom", notNullValue()));
    }

    @Test
    void getCommentairesByProduit_ShouldReturnEmptyList_WhenProductHasNoComments() throws Exception {
        // Given - Produit 1 n'a pas de commentaires
        Long produitId = produitId1;

        // When & Then
        mockMvc.perform(get("/api/commentaires/produit/{produitId}", produitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }


    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenContentIsEmpty() throws Exception {
        // Given
        Long produitId = produitId1;
        Map<String, String> request = new HashMap<>();
        request.put("contenu", ""); // Contenu vide

        // When & Then
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .header("X-Client-Id", clientId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenClientIdHeaderMissing() throws Exception {
        // Given
        Long produitId = produitId1;
        Map<String, String> request = new HashMap<>();
        request.put("contenu", "Test comment");

        // When & Then
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenInvalidClientId() throws Exception {
        // Given
        Long produitId = produitId1;
        Long invalidClientId = 999L;
        Map<String, String> request = new HashMap<>();
        request.put("contenu", "Test comment");

        // When & Then
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .header("X-Client-Id", invalidClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenInvalidProductId() throws Exception {
        // Given
        Long invalidProduitId = 999L;
        Map<String, String> request = new HashMap<>();
        request.put("contenu", "Test comment");

        // When & Then
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", invalidProduitId)
                        .header("X-Client-Id", clientId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }


    @Test
    void getNombreCommentaires_ShouldReturnCorrectCount() throws Exception {
        // Given - Produit 3 a 3 commentaires, Produit 2 a 1 commentaire, Produit 1 a 0 commentaire
        Long produitIdAvec3Commentaires = produitId3;
        Long produitIdAvec1Commentaire = produitId2;
        Long produitIdSansCommentaire = produitId1;

        // When & Then
        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", produitIdAvec3Commentaires))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", produitIdAvec1Commentaire))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", produitIdSansCommentaire))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getNombreCommentaires_ShouldReturnZero_WhenInvalidProductId() throws Exception {
        // Given
        Long invalidProduitId = 999L;

        // When & Then
        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", invalidProduitId))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }


    @Test
    void database_ShouldContainInitialComments() {
        // Test direct sur le repository
        List<Commentaire> commentaires = commentaireRepository.findAll();

        // Vérifier qu'il y a exactement 4 commentaires initiaux
        assertThat(commentaires).hasSize(4);

        // Vérifier les relations
        for (Commentaire commentaire : commentaires) {
            assertThat(commentaire.getClient()).isNotNull();
            assertThat(commentaire.getProduit()).isNotNull();
            assertThat(commentaire.getDate()).isNotNull();
            assertThat(commentaire.getContenu()).isNotBlank();
        }

        // Vérifier les comptes spécifiques
        assertThat(commentaireRepository.countByProduitId(produitId1)).isEqualTo(0);
        assertThat(commentaireRepository.countByProduitId(produitId2)).isEqualTo(1);
        assertThat(commentaireRepository.countByProduitId(produitId3)).isEqualTo(3);
        assertThat(commentaireRepository.countByProduitId(produitId4)).isEqualTo(0);
    }

    @Test
    void commentRepository_Methods_ShouldWorkCorrectly() {
        // Test des méthodes du repository
        List<Commentaire> commentairesProduit3 = commentaireRepository.findByProduitIdWithClient(produitId3);
        assertThat(commentairesProduit3).hasSize(3);

        // Vérifier que les clients sont bien chargés
        for (Commentaire commentaire : commentairesProduit3) {
            assertThat(commentaire.getClient()).isNotNull();
            assertThat(commentaire.getClient().getNom()).isNotBlank();
            assertThat(commentaire.getClient().getPrenom()).isNotBlank();
        }

        int count = commentaireRepository.countByProduitId(produitId3);
        assertThat(count).isEqualTo(3);

        // Test de recherche par ID avec client
        Optional<Commentaire> commentaireOpt = commentaireRepository.findByIdWithClient(commentairesProduit3.get(0).getIdcommentaire());
        assertThat(commentaireOpt).isPresent();
        assertThat(commentaireOpt.get().getClient()).isNotNull();
    }


}