package com.marketplace.interaction.controller;

import com.marketplace.interaction.service.InteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/interactions")
@CrossOrigin(origins = "http://localhost:4200")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @PostMapping("/toggle/{produitId}")
    public ResponseEntity<Map<String, Object>> toggleInteraction(
            @PathVariable Long produitId,
            @RequestHeader("X-Client-Id") Long clientId) {

        System.out.println("🎯 Requête reçue - toggleInteraction: produitId=" + produitId + ", clientId=" + clientId);

        try {
            boolean isLiked = interactionService.toggleInteraction(produitId, clientId);
            int newCount = interactionService.getInteractionCount(produitId);

            Map<String, Object> response = new HashMap<>();
            response.put("liked", isLiked);
            response.put("interactionCount", newCount);

            System.out.println("✅ Réponse: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("details", "Produit ID: " + produitId + ", Client ID: " + clientId);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/check/{produitId}")
    public ResponseEntity<Boolean> checkInteraction(
            @PathVariable Long produitId,
            @RequestHeader("X-Client-Id") Long clientId) {

        try {
            boolean hasLiked = interactionService.hasClientLikedProduct(produitId, clientId);
            return ResponseEntity.ok(hasLiked);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @GetMapping("/count/{produitId}")
    public ResponseEntity<Integer> getInteractionCount(@PathVariable Long produitId) {
        try {
            int count = interactionService.getInteractionCount(produitId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(0);
        }
    }
}