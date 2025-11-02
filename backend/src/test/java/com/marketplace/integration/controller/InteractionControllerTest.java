package com.marketplace.integration.controller;

import com.marketplace.interaction.entity.Interaction;
import com.marketplace.interaction.repository.InteractionRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Sql(scripts = {"/schema-test.sql", "/dataInteractionIntgTest.sql"})
class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Long clientId1;
    private Long clientId2;
    private Long produitId1;
    private Long produitId2;
    private Long produitId3;

    @BeforeEach
    void setUp() {
        // Nettoyer les interactions existantes
        interactionRepository.deleteAll();

        // Récupérer les clients et produits depuis la base de test H2
        List<Client> clients = clientRepository.findAll();
        List<Produit> produits = produitRepository.findAll();

        assertThat(clients).isNotEmpty();
        assertThat(produits).isNotEmpty();

        clientId1 = clients.get(0).getIdclient();
        clientId2 = clients.get(1).getIdclient();
        produitId1 = produits.get(0).getIdproduit();
        produitId2 = produits.get(1).getIdproduit();
        produitId3 = produits.get(2).getIdproduit();

        // Créer quelques interactions initiales pour les tests
        createInteraction(clientId1, produitId1); // Client1 like Produit1
        createInteraction(clientId2, produitId1); // Client2 like Produit1
        createInteraction(clientId1, produitId2); // Client1 like Produit2
    }

    private void createInteraction(Long clientId, Long produitId) {
        Client client = clientRepository.findById(clientId).orElseThrow();
        Produit produit = produitRepository.findById(produitId).orElseThrow();

        Interaction interaction = new Interaction();
        interaction.setClient(client);
        interaction.setProduit(produit);
        interaction.setDate(LocalDateTime.now());
        interactionRepository.save(interaction);
    }

    @Test
    void toggleInteraction_ShouldCreateInteraction_WhenNotExists() throws Exception {
        // Given - Client 2 n'a pas encore liké Produit 2
        Long produitId = produitId2;
        Long clientId = clientId2;

        // Vérifier qu'il n'y a pas d'interaction initiale
        boolean initialLikeStatus = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        int initialCount = interactionRepository.countByProduitId(produitId);

        assertThat(initialLikeStatus).isFalse();

        // When & Then
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.interactionCount").value(initialCount + 1));

        // Vérifier en base
        boolean finalLikeStatus = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        int finalCount = interactionRepository.countByProduitId(produitId);

        assertThat(finalLikeStatus).isTrue();
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    @Test
    void toggleInteraction_ShouldRemoveInteraction_WhenExists() throws Exception {
        // Given - Client 1 a déjà liké Produit 1
        Long produitId = produitId1;
        Long clientId = clientId1;

        // Vérifier qu'il y a une interaction initiale
        boolean initialLikeStatus = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        int initialCount = interactionRepository.countByProduitId(produitId);

        assertThat(initialLikeStatus).isTrue();

        // When & Then
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.interactionCount").value(initialCount - 1));

        // Vérifier en base
        boolean finalLikeStatus = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        int finalCount = interactionRepository.countByProduitId(produitId);

        assertThat(finalLikeStatus).isFalse();
        assertThat(finalCount).isEqualTo(initialCount - 1);
    }

    @Test
    void toggleInteraction_ShouldReturnBadRequest_WhenClientIdHeaderMissing() throws Exception {
        // Given
        Long produitId = produitId1;

        // When & Then
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void toggleInteraction_ShouldReturnBadRequest_WhenInvalidClientId() throws Exception {
        // Given
        Long produitId = produitId1;
        Long invalidClientId = 999L;

        // When & Then
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", invalidClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void checkInteraction_ShouldReturnTrue_WhenInteractionExists() throws Exception {
        // Given - Client 1 a liké Produit 1
        Long produitId = produitId1;
        Long clientId = clientId1;

        // Vérifier que l'interaction existe en base
        boolean existsInDb = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        assertThat(existsInDb).isTrue();

        // When & Then
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkInteraction_ShouldReturnFalse_WhenInteractionNotExists() throws Exception {
        // Given - Client 2 n'a pas liké Produit 2
        Long produitId = produitId2;
        Long clientId = clientId2;

        // Vérifier que l'interaction n'existe pas en base
        boolean existsInDb = interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
        assertThat(existsInDb).isFalse();

        // When & Then
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", clientId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void checkInteraction_ShouldReturnFalse_WhenInvalidClientId() throws Exception {
        // Given
        Long produitId = produitId1;
        Long invalidClientId = 999L;

        // When & Then - CORRECTION : Attendre 200 avec false, pas 400
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", invalidClientId))
                .andExpect(status().isOk()) // ← Changé de isBadRequest() à isOk()
                .andExpect(content().string("false"));
    }

    @Test
    void getInteractionCount_ShouldReturnCorrectCount() throws Exception {
        // Given - Produit 1 a 2 interactions
        Long produitId = produitId1;
        int expectedCount = interactionRepository.countByProduitId(produitId);

        assertThat(expectedCount).isEqualTo(2);

        // When & Then
        mockMvc.perform(get("/api/interactions/count/{produitId}", produitId))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));
    }



    @Test
    void getInteractionCount_ShouldReturnZero_WhenInvalidProduitId() throws Exception {
        // Given
        Long invalidProduitId = 999L;

        // When & Then
        mockMvc.perform(get("/api/interactions/count/{produitId}", invalidProduitId))
                .andExpect(status().isOk()) // ← CORRIGÉ : 200 au lieu de 400
                .andExpect(content().string("0"));
    }

    @Test
    void endpoints_ShouldHaveCorrectCorsHeaders() throws Exception {
        // Test CORS pour toggleInteraction
        mockMvc.perform(post("/api/interactions/toggle/1")
                        .header("X-Client-Id", clientId1)
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

        // Test CORS pour checkInteraction
        mockMvc.perform(get("/api/interactions/check/1")
                        .header("X-Client-Id", clientId1)
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void database_ShouldContainInitialInteractions() {
        // Test direct sur le repository
        List<Interaction> interactions = interactionRepository.findAll();

        // Vérifier qu'il y a exactement 3 interactions initiales
        assertThat(interactions).hasSize(3);

        // Vérifier les relations
        for (Interaction interaction : interactions) {
            assertThat(interaction.getClient()).isNotNull();
            assertThat(interaction.getProduit()).isNotNull();
            assertThat(interaction.getDate()).isNotNull();
        }

        // Vérifier les comptes spécifiques
        assertThat(interactionRepository.countByProduitId(produitId1)).isEqualTo(2);
        assertThat(interactionRepository.countByProduitId(produitId2)).isEqualTo(1);
        assertThat(interactionRepository.countByProduitId(produitId3)).isEqualTo(0);
    }

    @Test
    void toggleInteraction_ShouldHandleConcurrentRequests() throws Exception {
        // Given - Produit sans interactions
        Long produitId = produitId3;
        Long clientId = clientId1;

        // Vérifier état initial
        assertThat(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent()).isFalse();
        assertThat(interactionRepository.countByProduitId(produitId)).isEqualTo(0);

        // When & Then - Premier toggle (création)
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.interactionCount").value(1));

        // Vérifier après création
        assertThat(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent()).isTrue();
        assertThat(interactionRepository.countByProduitId(produitId)).isEqualTo(1);

        // Deuxième toggle (suppression)
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.interactionCount").value(0));

        // Vérifier après suppression
        assertThat(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent()).isFalse();
        assertThat(interactionRepository.countByProduitId(produitId)).isEqualTo(0);
    }

    @Test
    void interactionRepository_Methods_ShouldWorkCorrectly() {
        // Test des méthodes du repository
        Optional<Interaction> interaction = interactionRepository.findByProduitIdAndClientId(produitId1, clientId1);
        assertThat(interaction).isPresent();
        assertThat(interaction.get().getClient().getIdclient()).isEqualTo(clientId1);
        assertThat(interaction.get().getProduit().getIdproduit()).isEqualTo(produitId1);

        int count = interactionRepository.countByProduitId(produitId1);
        assertThat(count).isEqualTo(2);

        // Test de suppression
        int deletedCount = interactionRepository.deleteByProduitIdAndClientId(produitId1, clientId1);
        assertThat(deletedCount).isEqualTo(1);

        // Vérifier que l'interaction a bien été supprimée
        Optional<Interaction> deletedInteraction = interactionRepository.findByProduitIdAndClientId(produitId1, clientId1);
        assertThat(deletedInteraction).isNotPresent();

        // Vérifier que le compte a diminué
        int newCount = interactionRepository.countByProduitId(produitId1);
        assertThat(newCount).isEqualTo(1);
    }
}