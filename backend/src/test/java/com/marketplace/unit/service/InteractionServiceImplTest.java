package com.marketplace.unit.service;

import com.marketplace.interaction.entity.Interaction;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import com.marketplace.interaction.repository.InteractionRepository;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.repository.ClientRepository;
import com.marketplace.interaction.service.InteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

    @Mock
    private InteractionRepository interactionRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private InteractionService interactionService;

    private Produit produit;
    private Client client;
    private Interaction interaction;

    @BeforeEach
    void setUp() {
        // Initialisation des données de test
        produit = new Produit();
        produit.setIdproduit(1L);
        produit.setNom("Tapie");

        client = new Client();
        client.setId(1L);
        client.setNom("John Doe");

        interaction = new Interaction();
        interaction.setIdinteraction(1L);
        interaction.setProduit(produit);
        interaction.setClient(client);
        interaction.setDate(LocalDateTime.now());
    }

    @Test
    void toggleInteraction_ShouldCreateInteraction_WhenNoExistingInteraction() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 1L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.empty());
        when(produitRepository.findById(produitId))
                .thenReturn(Optional.of(produit));
        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(client));
        when(interactionRepository.save(any(Interaction.class)))
                .thenReturn(interaction);

        // Act
        boolean result = interactionService.toggleInteraction(produitId, clientId);

        // Assert
        assertTrue(result, "Devrait retourner true pour une nouvelle interaction");
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
        verify(produitRepository).findById(produitId);
        verify(clientRepository).findById(clientId);
        verify(interactionRepository).save(any(Interaction.class));
    }

    @Test
    void toggleInteraction_ShouldDeleteInteraction_WhenExistingInteraction() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 1L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.of(interaction));
        doNothing().when(interactionRepository).delete(interaction);

        // Act
        boolean result = interactionService.toggleInteraction(produitId, clientId);

        // Assert
        assertFalse(result, "Devrait retourner false pour une suppression d'interaction");
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
        verify(interactionRepository).delete(interaction);
        verify(interactionRepository, never()).save(any(Interaction.class));
    }

    @Test
    void toggleInteraction_ShouldThrowException_WhenProduitNotFound() {
        // Arrange
        Long produitId = 99L;
        Long clientId = 1L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.empty());
        when(produitRepository.findById(produitId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            interactionService.toggleInteraction(produitId, clientId);
        });

        assertEquals("Produit non trouvé avec l'ID: 99", exception.getMessage());
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
        verify(produitRepository).findById(produitId);
        verify(clientRepository, never()).findById(anyLong());
        verify(interactionRepository, never()).save(any(Interaction.class));
    }

    @Test
    void toggleInteraction_ShouldThrowException_WhenClientNotFound() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 99L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.empty());
        when(produitRepository.findById(produitId))
                .thenReturn(Optional.of(produit));
        when(clientRepository.findById(clientId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            interactionService.toggleInteraction(produitId, clientId);
        });

        assertEquals("Client non trouvé avec l'ID: 99", exception.getMessage());
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
        verify(produitRepository).findById(produitId);
        verify(clientRepository).findById(clientId);
        verify(interactionRepository, never()).save(any(Interaction.class));
    }

    @Test
    void hasClientLikedProduct_ShouldReturnTrue_WhenInteractionExists() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 1L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.of(interaction));

        // Act
        boolean result = interactionService.hasClientLikedProduct(produitId, clientId);

        // Assert
        assertTrue(result, "Devrait retourner true quand l'interaction existe");
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
    }

    @Test
    void hasClientLikedProduct_ShouldReturnFalse_WhenNoInteraction() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 1L;

        when(interactionRepository.findByProduitIdAndClientId(produitId, clientId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = interactionService.hasClientLikedProduct(produitId, clientId);

        // Assert
        assertFalse(result, "Devrait retourner false quand aucune interaction n'existe");
        verify(interactionRepository).findByProduitIdAndClientId(produitId, clientId);
    }

    @Test
    void getInteractionCount_ShouldReturnCount() {
        // Arrange
        Long produitId = 1L;
        int expectedCount = 5;

        when(interactionRepository.countByProduitId(produitId))
                .thenReturn(expectedCount);

        // Act
        int result = interactionService.getInteractionCount(produitId);

        // Assert
        assertEquals(expectedCount, result, "Devrait retourner le bon nombre d'interactions");
        verify(interactionRepository).countByProduitId(produitId);
    }

    @Test
    void toggleInteraction_ShouldHandleNullParameters() {
        // Arrange
        when(interactionRepository.findByProduitIdAndClientId(null, null))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            interactionService.toggleInteraction(null, null);
        });

        verify(interactionRepository).findByProduitIdAndClientId(null, null);
    }
}