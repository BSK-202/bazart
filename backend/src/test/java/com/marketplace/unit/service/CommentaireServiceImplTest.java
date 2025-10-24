package com.marketplace.unit.service;

import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.repository.CommentaireRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import com.marketplace.interaction.service.CommentaireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentaireServiceImplTest {

    @Mock
    private CommentaireRepository commentaireRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private CommentaireService commentaireService;

    private Produit produit;
    private Client client;
    private Commentaire commentaire;
    private Commentaire commentaire2;

    @BeforeEach
    void setUp() {
        // Initialisation des données de test
        produit = new Produit();
        produit.setIdproduit(1L);
        produit.setNom("Produit Test");

        client = new Client();
        client.setIdclient(1L);
        client.setNom("John Doe");
        client.setEmail("john@example.com");

        commentaire = new Commentaire();
        commentaire.setIdcommentaire(1L);
        commentaire.setContenu("Excellent produit !");
        commentaire.setDate(LocalDateTime.now());
        commentaire.setProduit(produit);
        commentaire.setClient(client);

        commentaire2 = new Commentaire();
        commentaire2.setIdcommentaire(2L);
        commentaire2.setContenu("Très satisfait");
        commentaire2.setDate(LocalDateTime.now().minusDays(1));
        commentaire2.setProduit(produit);
        commentaire2.setClient(client);
    }

    @Test
    void getCommentairesByProduit_ShouldReturnCommentairesList() {
        // Arrange
        Long produitId = 1L;
        List<Commentaire> expectedCommentaires = Arrays.asList(commentaire, commentaire2);

        when(commentaireRepository.findByProduitIdWithClient(produitId))
                .thenReturn(expectedCommentaires);

        // Act
        List<Commentaire> result = commentaireService.getCommentairesByProduit(produitId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Excellent produit !", result.get(0).getContenu());
        assertEquals("Très satisfait", result.get(1).getContenu());
        verify(commentaireRepository).findByProduitIdWithClient(produitId);
    }

    @Test
    void getCommentairesByProduit_ShouldReturnEmptyList_WhenNoCommentaires() {
        // Arrange
        Long produitId = 99L;
        List<Commentaire> emptyList = Arrays.asList();

        when(commentaireRepository.findByProduitIdWithClient(produitId))
                .thenReturn(emptyList);

        // Act
        List<Commentaire> result = commentaireService.getCommentairesByProduit(produitId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(commentaireRepository).findByProduitIdWithClient(produitId);
    }

    @Test
    void addCommentaire_ShouldCreateCommentaire_WhenProduitAndClientExist() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 1L;
        String contenu = "Nouveau commentaire";

        // Créer un nouveau commentaire pour le test (pas celui du setUp)
        Commentaire nouveauCommentaire = new Commentaire();
        nouveauCommentaire.setIdcommentaire(3L);
        nouveauCommentaire.setContenu(contenu);
        nouveauCommentaire.setDate(LocalDateTime.now());
        nouveauCommentaire.setProduit(produit);
        nouveauCommentaire.setClient(client);

        when(produitRepository.findById(produitId))
                .thenReturn(Optional.of(produit));
        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(client));
        when(commentaireRepository.save(any(Commentaire.class)))
                .thenReturn(nouveauCommentaire);
        when(commentaireRepository.findByIdWithClient(nouveauCommentaire.getIdcommentaire()))
                .thenReturn(Optional.of(nouveauCommentaire));

        // Act
        Commentaire result = commentaireService.addCommentaire(produitId, clientId, contenu);

        // Assert
        assertNotNull(result);
        assertEquals(contenu, result.getContenu()); // Maintenant ça doit correspondre
        assertEquals(produit, result.getProduit());
        assertEquals(client, result.getClient());
        assertNotNull(result.getDate());

        verify(produitRepository).findById(produitId);
        verify(clientRepository).findById(clientId);
        verify(commentaireRepository).save(any(Commentaire.class));
        verify(commentaireRepository).findByIdWithClient(nouveauCommentaire.getIdcommentaire());
    }

    @Test
    void addCommentaire_ShouldThrowException_WhenProduitNotFound() {
        // Arrange
        Long produitId = 99L;
        Long clientId = 1L;
        String contenu = "Nouveau commentaire";

        when(produitRepository.findById(produitId))
                .thenReturn(Optional.empty());
        // On ne mock pas clientRepository.findById car il ne sera pas atteint si produit n'existe pas

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentaireService.addCommentaire(produitId, clientId, contenu);
        });

        assertEquals("Produit ou Client non trouvé", exception.getMessage());
        verify(produitRepository).findById(produitId);
        // On ne vérifie pas clientRepository car l'exception est lancée après la vérification des deux
        verify(commentaireRepository, never()).save(any(Commentaire.class));
    }

    @Test
    void addCommentaire_ShouldThrowException_WhenClientNotFound() {
        // Arrange
        Long produitId = 1L;
        Long clientId = 99L;
        String contenu = "Nouveau commentaire";

        when(produitRepository.findById(produitId))
                .thenReturn(Optional.of(produit));
        when(clientRepository.findById(clientId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentaireService.addCommentaire(produitId, clientId, contenu);
        });

        assertEquals("Produit ou Client non trouvé", exception.getMessage());
        verify(produitRepository).findById(produitId);
        verify(clientRepository).findById(clientId);
        verify(commentaireRepository, never()).save(any(Commentaire.class));
    }

    @Test
    void deleteCommentaire_ShouldDelete_WhenUserIsOwner() {
        // Arrange
        Long commentaireId = 1L;
        Long clientId = 1L;

        when(commentaireRepository.findById(commentaireId))
                .thenReturn(Optional.of(commentaire));
        doNothing().when(commentaireRepository).deleteById(commentaireId);

        // Act
        commentaireService.deleteCommentaire(commentaireId, clientId);

        // Assert
        verify(commentaireRepository).findById(commentaireId);
        verify(commentaireRepository).deleteById(commentaireId);
    }

    @Test
    void deleteCommentaire_ShouldThrowException_WhenUserIsNotOwner() {
        // Arrange
        Long commentaireId = 1L;
        Long differentClientId = 2L; // Différent du client propriétaire

        when(commentaireRepository.findById(commentaireId))
                .thenReturn(Optional.of(commentaire));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentaireService.deleteCommentaire(commentaireId, differentClientId);
        });

        assertEquals("Non autorisé à supprimer ce commentaire", exception.getMessage());
        verify(commentaireRepository).findById(commentaireId);
        verify(commentaireRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteCommentaire_ShouldThrowException_WhenCommentaireNotFound() {
        // Arrange
        Long commentaireId = 99L;
        Long clientId = 1L;

        when(commentaireRepository.findById(commentaireId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentaireService.deleteCommentaire(commentaireId, clientId);
        });

        assertEquals("Commentaire non trouvé", exception.getMessage());
        verify(commentaireRepository).findById(commentaireId);
        verify(commentaireRepository, never()).deleteById(anyLong());
    }

    @Test
    void getNombreCommentaires_ShouldReturnCount() {
        // Arrange
        Long produitId = 1L;
        int expectedCount = 5;

        when(commentaireRepository.countByProduitId(produitId))
                .thenReturn(expectedCount);

        // Act
        int result = commentaireService.getNombreCommentaires(produitId);

        // Assert
        assertEquals(expectedCount, result);
        verify(commentaireRepository).countByProduitId(produitId);
    }

    @Test
    void getNombreCommentaires_ShouldReturnZero_WhenNoCommentaires() {
        // Arrange
        Long produitId = 99L;
        int expectedCount = 0;

        when(commentaireRepository.countByProduitId(produitId))
                .thenReturn(expectedCount);

        // Act
        int result = commentaireService.getNombreCommentaires(produitId);

        // Assert
        assertEquals(0, result);
        verify(commentaireRepository).countByProduitId(produitId);
    }
}