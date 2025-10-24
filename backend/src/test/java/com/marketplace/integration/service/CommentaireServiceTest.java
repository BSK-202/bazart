package com.marketplace.integration.service;

import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.repository.CommentaireRepository;
import com.marketplace.interaction.service.CommentaireService;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CommentaireService.class)
@Sql(scripts = {"/schema-test.sql", "/dataCommentaire-test.sql"})
class CommentaireServiceTest {

    @Autowired
    private CommentaireService commentaireService;

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void testGetCommentairesByProduit_ShouldReturnCommentsWithClientInfo() {
        // Given - Le produit 3 a 3 commentaires dans les données de test
        Long produitId = 3L;

        // When
        List<Commentaire> commentaires = commentaireService.getCommentairesByProduit(produitId);

        // Then
        assertNotNull(commentaires);
        assertEquals(3, commentaires.size());

        // Vérifier que les clients sont bien chargés
        commentaires.forEach(commentaire -> {
            assertNotNull(commentaire.getClient());
            assertNotNull(commentaire.getClient().getNom());
            assertNotNull(commentaire.getClient().getPrenom());
        });
    }

    @Test
    void testGetCommentairesByProduit_WhenNoComments_ShouldReturnEmptyList() {
        // Given - Le produit 4 n'a pas de commentaires
        Long produitId = 4L;

        // When
        List<Commentaire> commentaires = commentaireService.getCommentairesByProduit(produitId);

        // Then
        assertNotNull(commentaires);
        assertTrue(commentaires.isEmpty());
    }

    @Test
    void testAddCommentaire_ShouldCreateNewComment() {
        // Given
        Long produitId = 1L; // Produit sans commentaires
        Long clientId = 2L;
        String contenu = "Nouveau commentaire de test";

        int initialCount = commentaireRepository.countByProduitId(produitId);

        // When
        Commentaire result = commentaireService.addCommentaire(produitId, clientId, contenu);

        // Then
        assertNotNull(result);
        assertEquals(contenu, result.getContenu());
        assertNotNull(result.getDate());
        assertNotNull(result.getClient());
        assertEquals(clientId, result.getClient().getIdclient());
        assertNotNull(result.getProduit());
        assertEquals(produitId, result.getProduit().getIdproduit());

        // Vérifier que le commentaire est bien persisté
        assertEquals(initialCount + 1, commentaireRepository.countByProduitId(produitId));
    }

    @Test
    void testAddCommentaire_WhenProductNotFound_ShouldThrowException() {
        // Given
        Long produitIdInexistant = 999L;
        Long clientId = 1L;
        String contenu = "Commentaire test";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentaireService.addCommentaire(produitIdInexistant, clientId, contenu));
        assertTrue(exception.getMessage().contains("Produit ou Client non trouvé"));
    }

    @Test
    void testAddCommentaire_WhenClientNotFound_ShouldThrowException() {
        // Given
        Long produitId = 1L;
        Long clientIdInexistant = 999L;
        String contenu = "Commentaire test";

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentaireService.addCommentaire(produitId, clientIdInexistant, contenu));
        assertTrue(exception.getMessage().contains("Produit ou Client non trouvé"));
    }

    @Test
    void testDeleteCommentaire_WhenOwner_ShouldDeleteComment() {
        // Given - Récupérer un commentaire existant dynamiquement
        List<Commentaire> commentaires = commentaireRepository.findAll();
        assertFalse(commentaires.isEmpty(), "Aucun commentaire trouvé dans la base de test");

        Commentaire commentaireExistant = commentaires.get(0);
        Long commentaireId = commentaireExistant.getIdcommentaire();
        Long clientId = commentaireExistant.getClient().getIdclient();
        Long produitId = commentaireExistant.getProduit().getIdproduit();

        int initialCount = commentaireRepository.countByProduitId(produitId);

        // When
        commentaireService.deleteCommentaire(commentaireId, clientId);

        // Then - Le commentaire doit être supprimé
        assertFalse(commentaireRepository.findById(commentaireId).isPresent());
        assertEquals(initialCount - 1, commentaireRepository.countByProduitId(produitId));
    }

    @Test
    void testDeleteCommentaire_WhenNotOwner_ShouldThrowException() {
        // Given - Récupérer un commentaire existant
        List<Commentaire> commentaires = commentaireRepository.findAll();
        assertFalse(commentaires.isEmpty(), "Aucun commentaire trouvé dans la base de test");

        Commentaire commentaireExistant = commentaires.get(0);
        Long commentaireId = commentaireExistant.getIdcommentaire();
        Long clientIdNotOwner = 999L; // ID qui n'est pas le propriétaire

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentaireService.deleteCommentaire(commentaireId, clientIdNotOwner));
        assertTrue(exception.getMessage().contains("Non autorisé à supprimer ce commentaire"));
    }

    @Test
    void testDeleteCommentaire_WhenCommentNotFound_ShouldThrowException() {
        // Given
        Long commentaireIdInexistant = 999L;
        Long clientId = 1L;

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentaireService.deleteCommentaire(commentaireIdInexistant, clientId));
        assertTrue(exception.getMessage().contains("Commentaire non trouvé"));
    }

    @Test
    void testGetNombreCommentaires_ShouldReturnCorrectCount() {
        // Given - Produit 3 a 3 commentaires, Produit 2 a 1 commentaire, Produit 1 a 0 commentaire
        Long produitIdAvec3Commentaires = 3L;
        Long produitIdAvec1Commentaire = 2L;
        Long produitIdSansCommentaire = 1L;

        // When & Then
        assertEquals(3, commentaireService.getNombreCommentaires(produitIdAvec3Commentaires));
        assertEquals(1, commentaireService.getNombreCommentaires(produitIdAvec1Commentaire));
        assertEquals(0, commentaireService.getNombreCommentaires(produitIdSansCommentaire));
    }

    @Test
    void testMultipleClientsCommentSameProduct() {
        // Given
        Long produitId = 4L; // Produit sans commentaires
        Long client1Id = 1L;
        Long client2Id = 2L;
        String contenu1 = "Premier commentaire";
        String contenu2 = "Deuxième commentaire";

        // When - Client 1 ajoute un commentaire
        Commentaire comment1 = commentaireService.addCommentaire(produitId, client1Id, contenu1);

        // Then
        assertNotNull(comment1);
        assertEquals(contenu1, comment1.getContenu());
        assertEquals(1, commentaireService.getNombreCommentaires(produitId));

        // When - Client 2 ajoute un commentaire
        Commentaire comment2 = commentaireService.addCommentaire(produitId, client2Id, contenu2);

        // Then
        assertNotNull(comment2);
        assertEquals(contenu2, comment2.getContenu());
        assertEquals(2, commentaireService.getNombreCommentaires(produitId));

        // Vérifier que les deux commentaires sont récupérés
        List<Commentaire> commentaires = commentaireService.getCommentairesByProduit(produitId);
        assertEquals(2, commentaires.size());
    }

    @Test
    void testDeleteCommentaire_IntegrationFlow() {
        // Given - Créer un nouveau commentaire pour avoir un ID connu
        Long produitId = 1L;
        Long clientId = 1L;
        String contenu = "Commentaire à supprimer";

        Commentaire nouveauCommentaire = commentaireService.addCommentaire(produitId, clientId, contenu);
        Long nouveauCommentaireId = nouveauCommentaire.getIdcommentaire();

        // Vérifier qu'il existe avant suppression
        assertTrue(commentaireRepository.findById(nouveauCommentaireId).isPresent());
        int initialCount = commentaireRepository.countByProduitId(produitId);

        // When
        commentaireService.deleteCommentaire(nouveauCommentaireId, clientId);

        // Then
        assertFalse(commentaireRepository.findById(nouveauCommentaireId).isPresent());
        assertEquals(initialCount - 1, commentaireRepository.countByProduitId(produitId));
    }
}