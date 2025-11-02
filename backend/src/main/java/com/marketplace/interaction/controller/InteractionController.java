package com.marketplace.interaction.controller;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.interaction.service.InteractionService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/interactions")
@CrossOrigin(origins = "http://localhost:4200")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private NotificationService notificationService;

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

            // === NOTIFICATION LOGIC: Notify product owner about new like/favorite ===
            Optional<Produit> produitOpt = produitService.getProduitById(produitId);
            if (produitOpt.isPresent()) {
                Produit produit = produitOpt.get();
                Long vendeurId = produit.getVendeur().getIdclient();
                if (isLiked && !vendeurId.equals(clientId)) { // Only notify when liked, and not self-like
                    Optional<Client> clientOpt = clientService.getClientById(clientId);
                    String likerName = clientOpt.map(c -> c.getPrenom() + " " + c.getNom()).orElse("Un utilisateur");
                    Map<String, Object> notifData = new HashMap<>();
                    notifData.put("productName", produit.getNom());
                    notifData.put("message", likerName + " a ajouté votre produit à ses favoris.");
                    notifData.put("likerId", clientId);

                    notificationService.processEvent(
                            NotificationType.MESSAGE,
                            Set.of(vendeurId),
                            notifData
                    );
                }
            }
            // === END NOTIFICATION LOGIC ===

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