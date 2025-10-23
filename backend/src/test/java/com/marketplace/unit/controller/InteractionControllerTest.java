package com.marketplace.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.interaction.controller.InteractionController;
import com.marketplace.interaction.service.InteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InteractionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InteractionService interactionService;

    private ObjectMapper objectMapper;
    private Long produitId;
    private Long clientId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        produitId = 1L;
        clientId = 1L;
    }

    @Test
    void toggleInteraction_ShouldReturnSuccessResponse_WhenValidRequest() throws Exception {
        // Arrange
        boolean isLiked = true;
        int interactionCount = 5;

        when(interactionService.toggleInteraction(produitId, clientId)).thenReturn(isLiked);
        when(interactionService.getInteractionCount(produitId)).thenReturn(interactionCount);

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("liked", isLiked);
        expectedResponse.put("interactionCount", interactionCount);

        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.interactionCount").value(5));

        verify(interactionService).toggleInteraction(produitId, clientId);
        verify(interactionService).getInteractionCount(produitId);
    }

    @Test
    void toggleInteraction_ShouldReturnUnlikedResponse_WhenInteractionRemoved() throws Exception {
        // Arrange
        boolean isLiked = false; // Unlike
        int interactionCount = 4;

        when(interactionService.toggleInteraction(produitId, clientId)).thenReturn(isLiked);
        when(interactionService.getInteractionCount(produitId)).thenReturn(interactionCount);

        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.interactionCount").value(4));

        verify(interactionService).toggleInteraction(produitId, clientId);
        verify(interactionService).getInteractionCount(produitId);
    }

    @Test
    void toggleInteraction_ShouldReturnBadRequest_WhenServiceThrowsException() throws Exception {
        // Arrange
        String errorMessage = "Produit non trouvé";
        when(interactionService.toggleInteraction(produitId, clientId))
                .thenThrow(new RuntimeException(errorMessage));

        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(errorMessage))
                .andExpect(jsonPath("$.details").exists());

        verify(interactionService).toggleInteraction(produitId, clientId);
        verify(interactionService, never()).getInteractionCount(anyLong());
    }

    @Test
    void toggleInteraction_ShouldReturnBadRequest_WhenMissingClientIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Spring retourne 400 quand @RequestHeader manque
    }

    @Test
    void checkInteraction_ShouldReturnTrue_WhenClientLikedProduct() throws Exception {
        // Arrange
        when(interactionService.hasClientLikedProduct(produitId, clientId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(interactionService).hasClientLikedProduct(produitId, clientId);
    }

    @Test
    void checkInteraction_ShouldReturnFalse_WhenClientNotLikedProduct() throws Exception {
        // Arrange
        when(interactionService.hasClientLikedProduct(produitId, clientId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(interactionService).hasClientLikedProduct(produitId, clientId);
    }

    @Test
    void checkInteraction_ShouldReturnFalse_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(interactionService.hasClientLikedProduct(produitId, clientId))
                .thenThrow(new RuntimeException("Erreur de base de données"));

        // Act & Assert
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));

        verify(interactionService).hasClientLikedProduct(produitId, clientId);
    }

    @Test
    void getInteractionCount_ShouldReturnCount_WhenValidRequest() throws Exception {
        // Arrange
        int count = 10;
        when(interactionService.getInteractionCount(produitId)).thenReturn(count);

        // Act & Assert
        mockMvc.perform(get("/api/interactions/count/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));

        verify(interactionService).getInteractionCount(produitId);
    }

    @Test
    void getInteractionCount_ShouldReturnZero_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(interactionService.getInteractionCount(produitId))
                .thenThrow(new RuntimeException("Produit non trouvé"));

        // Act & Assert
        mockMvc.perform(get("/api/interactions/count/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("0"));

        verify(interactionService).getInteractionCount(produitId);
    }

    @Test
    void toggleInteraction_ShouldHandleDifferentClientIds() throws Exception {
        // Arrange
        Long differentClientId = 2L;
        when(interactionService.toggleInteraction(produitId, differentClientId)).thenReturn(true);
        when(interactionService.getInteractionCount(produitId)).thenReturn(6);

        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", produitId)
                        .header("X-Client-Id", differentClientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.interactionCount").value(6));

        verify(interactionService).toggleInteraction(produitId, differentClientId);
    }

    @Test
    void toggleInteraction_ShouldHandleDifferentProductIds() throws Exception {
        // Arrange
        Long differentProduitId = 99L;
        when(interactionService.toggleInteraction(differentProduitId, clientId)).thenReturn(false);
        when(interactionService.getInteractionCount(differentProduitId)).thenReturn(0);

        // Act & Assert
        mockMvc.perform(post("/api/interactions/toggle/{produitId}", differentProduitId)
                        .header("X-Client-Id", clientId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.interactionCount").value(0));

        verify(interactionService).toggleInteraction(differentProduitId, clientId);
    }

    @Test
    void checkInteraction_ShouldReturnBadRequest_WhenMissingClientIdHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/interactions/check/{produitId}", produitId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}