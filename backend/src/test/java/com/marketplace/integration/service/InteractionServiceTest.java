package com.marketplace.integration.service;

import com.marketplace.interaction.repository.InteractionRepository;
import com.marketplace.interaction.service.InteractionService;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(InteractionService.class)
@Sql(scripts = {"/schema-test.sql", "/dataInteraction-test.sql"})
class InteractionServiceTest {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void testToggleInteraction_WhenNoExistingInteraction_ShouldCreateInteraction() {
        // Given
        Long produitId = (Long) 4L; // Nouveau produit non utilisé
        Long clientId = (Long) 2L;

        // Vérifier qu'il n'y a pas d'interaction initiale
        assertFalse(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent());

        // When
        boolean result = interactionService.toggleInteraction(produitId, clientId);

        // Then
        assertTrue(result); // Retourne true quand une interaction est créée
        assertTrue(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent());
        assertEquals(1, interactionRepository.countByProduitId(produitId));
    }

    @Test
    void testToggleInteraction_WhenExistingInteraction_ShouldDeleteInteraction() {
        // Given - Une interaction existe déjà (doit être définie dans data-test-interaction.sql)
        Long produitId = (Long) 2L;
        Long clientId = (Long) 1L;

        // Vérifier qu'il y a une interaction initiale
        assertTrue(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent());
        int initialCount = interactionRepository.countByProduitId(produitId);

        // When
        boolean result = interactionService.toggleInteraction(produitId, clientId);

        // Then
        assertFalse(result); // Retourne false quand une interaction est supprimée
        assertFalse(interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent());
        assertEquals(initialCount - 1, interactionRepository.countByProduitId(produitId));
    }

    @Test
    void testToggleInteraction_WhenProductNotFound_ShouldThrowException() {
        // Given
        Long produitIdInexistant = (Long) 999L;
        Long clientId = (Long) 1L;

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> interactionService.toggleInteraction(produitIdInexistant, clientId));
        assertTrue(exception.getMessage().contains("Produit non trouvé"));
    }

    @Test
    void testToggleInteraction_WhenClientNotFound_ShouldThrowException() {
        // Given
        Long produitId = (Long) 1L;
        Long clientIdInexistant = (Long) 999L;

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> interactionService.toggleInteraction(produitId, clientIdInexistant));
        assertTrue(exception.getMessage().contains("Client non trouvé"));
    }

    @Test
    void testHasClientLikedProduct_WhenLiked_ShouldReturnTrue() {
        // Given - Le client 1 a liké le produit 2 (doit être défini dans data-test-interaction.sql)
        Long produitId = (Long) 2L;
        Long clientId = (Long) 1L;

        // When
        boolean result = interactionService.hasClientLikedProduct(produitId, clientId);

        // Then
        assertTrue(result);
    }

    @Test
    void testHasClientLikedProduct_WhenNotLiked_ShouldReturnFalse() {
        // Given - Le client 1 n'a pas liké le produit 1
        Long produitId = (Long) 1L;
        Long clientId = (Long) 1L;

        // When
        boolean result = interactionService.hasClientLikedProduct(produitId, clientId);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetInteractionCount_ShouldReturnCorrectCount() {
        // Given - Le produit 2 a 1 like, le produit 3 a 2 likes (doit être défini dans data-test-interaction.sql)
        Long produitIdAvec1Like = (Long) 2L;
        Long produitIdAvec2Likes = (Long) 3L;
        Long produitIdSansLike = (Long) 1L;

        // When & Then
        assertEquals(1, interactionService.getInteractionCount(produitIdAvec1Like));
        assertEquals(2, interactionService.getInteractionCount(produitIdAvec2Likes));
        assertEquals(0, interactionService.getInteractionCount(produitIdSansLike));
    }

    @Test
    void testMultipleClientsInteractingWithSameProduct() {
        // Given
        Long produitId = (Long) 4L; // Nouveau produit non utilisé
        Long client1Id = (Long) 1L;
        Long client2Id = (Long) 2L;

        // When - Client 1 like le produit
        boolean result1 = interactionService.toggleInteraction(produitId, client1Id);

        // Then
        assertTrue(result1);
        assertTrue(interactionService.hasClientLikedProduct(produitId, client1Id));
        assertEquals(1, interactionService.getInteractionCount(produitId));

        // When - Client 2 like le produit
        boolean result2 = interactionService.toggleInteraction(produitId, client2Id);

        // Then
        assertTrue(result2);
        assertTrue(interactionService.hasClientLikedProduct(produitId, client2Id));
        assertEquals(2, interactionService.getInteractionCount(produitId));

        // When - Client 1 unlike le produit
        boolean result3 = interactionService.toggleInteraction(produitId, client1Id);

        // Then
        assertFalse(result3);
        assertFalse(interactionService.hasClientLikedProduct(produitId, client1Id));
        assertTrue(interactionService.hasClientLikedProduct(produitId, client2Id));
        assertEquals(1, interactionService.getInteractionCount(produitId));
    }
}