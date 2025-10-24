package com.marketplace.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.interaction.controller.CommentaireController;
import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.service.CommentaireService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentaireController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentaireService commentaireService;

    @MockitoBean
    private ClientRepository clientRepository;

    private ObjectMapper objectMapper;
    private Long produitId;
    private Long clientId;
    private Commentaire commentaire;
    private Client client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        produitId = 1L;
        clientId = 1L;

        // Configuration du client
        client = new Client();
        client.setIdclient(clientId);
        client.setEmail("test@example.com");
        client.setPrenom("John");
        client.setNom("Doe");

        // Configuration du commentaire
        commentaire = new Commentaire();
        commentaire.setIdcommentaire(1L);
        commentaire.setContenu("Excellent produit !");
        commentaire.setDate(LocalDateTime.now());
        commentaire.setClient(client);
    }

    private void mockAuthentication() {
        // Mock de l'authentification
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);

        // Mock du repository client
        when(clientRepository.findByEmail("test@example.com")).thenReturn(Optional.of(client));
    }

    private void mockUnauthenticated() {
        // Mock pour utilisateur non authentifié
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCommentairesByProduit_ShouldReturnCommentairesList() throws Exception {
        // Arrange
        List<Commentaire> commentaires = Arrays.asList(commentaire);
        when(commentaireService.getCommentairesByProduit(produitId)).thenReturn(commentaires);

        // Act & Assert
        mockMvc.perform(get("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idcommentaire").value(1))
                .andExpect(jsonPath("$[0].contenu").value("Excellent produit !"));

        verify(commentaireService).getCommentairesByProduit(produitId);
    }

    @Test
    void getCommentairesByProduit_ShouldReturnEmptyList_WhenNoCommentaires() throws Exception {
        // Arrange
        when(commentaireService.getCommentairesByProduit(produitId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(commentaireService).getCommentairesByProduit(produitId);
    }



    @Test
    void addCommentaire_ShouldCreateCommentaire_WhenValidRequest() throws Exception {
        // Arrange
        mockAuthentication();

        String contenu = "Nouveau commentaire";
        Map<String, String> request = new HashMap<>();
        request.put("contenu", contenu);

        when(commentaireService.addCommentaire(produitId, clientId, contenu))
                .thenReturn(commentaire);

        // Act & Assert
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idcommentaire").value(1))
                .andExpect(jsonPath("$.contenu").value("Excellent produit !"));

        verify(commentaireService).addCommentaire(produitId, clientId, contenu);
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenEmptyContent() throws Exception {
        // Arrange
        mockAuthentication();

        Map<String, String> request = new HashMap<>();
        request.put("contenu", ""); // Contenu vide

        // Act & Assert
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le contenu ne peut pas être vide")); // CORRECTION : Vérifier le texte directement

        verify(commentaireService, never()).addCommentaire(anyLong(), anyLong(), anyString());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenContentIsNull() throws Exception {
        // Arrange
        mockAuthentication();

        Map<String, String> request = new HashMap<>();
        request.put("contenu", null); // Contenu null

        // Act & Assert
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le contenu ne peut pas être vide")); // CORRECTION : Vérifier le texte directement

        verify(commentaireService, never()).addCommentaire(anyLong(), anyLong(), anyString());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenServiceThrowsException() throws Exception {
        // Arrange
        mockAuthentication();

        String contenu = "Nouveau commentaire";
        Map<String, String> request = new HashMap<>();
        request.put("contenu", contenu);

        when(commentaireService.addCommentaire(produitId, clientId, contenu))
                .thenThrow(new RuntimeException("Produit non trouvé"));

        // Act & Assert
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Produit non trouvé"));

        verify(commentaireService).addCommentaire(produitId, clientId, contenu);
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenUserNotAuthenticated() throws Exception {
        // Arrange - Utilisateur non authentifié
        mockUnauthenticated();

        String contenu = "Nouveau commentaire";
        Map<String, String> request = new HashMap<>();
        request.put("contenu", contenu);

        // Act & Assert - CORRECTION : Vérifier le statut 400 (Bad Request) au lieu de 500
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // CORRECTION : 400 au lieu de 500
                .andExpect(jsonPath("$.error").exists());

        verify(commentaireService, never()).addCommentaire(anyLong(), anyLong(), anyString());
    }

    @Test
    void deleteCommentaire_ShouldDelete_WhenUserIsOwner() throws Exception {
        // Arrange
        mockAuthentication();
        Long commentaireId = 1L;

        doNothing().when(commentaireService).deleteCommentaire(commentaireId, clientId);

        // Act & Assert
        mockMvc.perform(delete("/api/commentaires/{commentaireId}", commentaireId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commentaireService).deleteCommentaire(commentaireId, clientId);
    }

    @Test
    void deleteCommentaire_ShouldReturnBadRequest_WhenServiceThrowsException() throws Exception {
        // Arrange
        mockAuthentication();
        Long commentaireId = 1L;

        doThrow(new RuntimeException("Commentaire non trouvé"))
                .when(commentaireService).deleteCommentaire(commentaireId, clientId);

        // Act & Assert
        mockMvc.perform(delete("/api/commentaires/{commentaireId}", commentaireId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Erreur: Commentaire non trouvé"));

        verify(commentaireService).deleteCommentaire(commentaireId, clientId);
    }

    @Test
    void getNombreCommentaires_ShouldReturnCount() throws Exception {
        // Arrange
        int expectedCount = 5;
        when(commentaireService.getNombreCommentaires(produitId)).thenReturn(expectedCount);

        // Act & Assert
        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));

        verify(commentaireService).getNombreCommentaires(produitId);
    }

    @Test
    void getNombreCommentaires_ShouldReturnZero_WhenNoCommentaires() throws Exception {
        // Arrange
        when(commentaireService.getNombreCommentaires(produitId)).thenReturn(0);

        // Act & Assert
        mockMvc.perform(get("/api/commentaires/produit/{produitId}/count", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(commentaireService).getNombreCommentaires(produitId);
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenClientNotFound() throws Exception {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("unknown@example.com");
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);

        // Simuler un client non trouvé
        when(clientRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        String contenu = "Nouveau commentaire";
        Map<String, String> request = new HashMap<>();
        request.put("contenu", contenu);

        // Act & Assert - CORRECTION : Vérifier le statut 400 (Bad Request) au lieu de 500
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // CORRECTION : 400 au lieu de 500
                .andExpect(jsonPath("$.error").exists());

        verify(commentaireService, never()).addCommentaire(anyLong(), anyLong(), anyString());
    }

    @Test
    void addCommentaire_ShouldReturnBadRequest_WhenMissingContentField() throws Exception {
        // Arrange
        mockAuthentication();

        Map<String, String> request = new HashMap<>(); // Pas de champ "contenu"

        // Act & Assert
        mockMvc.perform(post("/api/commentaires/produit/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le contenu ne peut pas être vide"));

        verify(commentaireService, never()).addCommentaire(anyLong(), anyLong(), anyString());
    }
}