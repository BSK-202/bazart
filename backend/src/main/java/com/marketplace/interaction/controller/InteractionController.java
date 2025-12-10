package com.marketplace.interaction.controller;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.interaction.entity.Interaction;
import com.marketplace.interaction.service.InteractionService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @GetMapping("/client/{clientId}/produits-likes")
    public ResponseEntity<List<Map<String, Object>>> getProduitsLikesByClient(@PathVariable Long clientId) {
        try {
            System.out.println("🎯 Récupération des produits likés pour le client: " + clientId);

            List<Interaction> interactions = interactionService.getInteractionsByClientId(clientId);

            // ✅ DEBUG: Afficher le nombre d'interactions trouvées
            System.out.println("📊 Nombre d'interactions trouvées: " + interactions.size());

            List<Map<String, Object>> produitsLikes = interactions.stream()
                    .map(interaction -> {
                        Produit produit = interaction.getProduit();

                        // ✅ DEBUG: Vérifier chaque produit
                        System.out.println("📦 Produit trouvé: " + produit.getNom() + " (ID: " + produit.getIdproduit() + ")");

                        Map<String, Object> produitMap = new HashMap<>();

                        produitMap.put("id", produit.getIdproduit());
                        produitMap.put("nom", produit.getNom());
                        produitMap.put("description", produit.getDescription());
                        produitMap.put("prixDebut", produit.getPrixDebut());
                        produitMap.put("prixFin", produit.getPrixFin());
                        produitMap.put("etat", produit.getEtat());
                        produitMap.put("datePublication", produit.getDatePublication());
                        produitMap.put("dateEnchere", produit.getDateEnchere());
                        produitMap.put("dureeEnchereJours", produit.getDureeEnchereJours());

                        // Informations vendeur
                        if (produit.getVendeur() != null) {
                            produitMap.put("vendeurNom", produit.getVendeur().getNom());
                            produitMap.put("vendeurId", produit.getVendeur().getIdclient());
                        } else {
                            produitMap.put("vendeurNom", "Inconnu");
                        }

                        // Informations catégorie
                        if (produit.getCategorie() != null) {
                            produitMap.put("categorieNom", produit.getCategorie().getNomCategorie());
                        } else {
                            produitMap.put("categorieNom", "Non catégorisé");
                        }

                        // Images
                        if (produit.getImages() != null && !produit.getImages().isEmpty()) {
                            List<String> imagesUrls = produit.getImages().stream()
                                    .map(ProduitImage::getUrl)
                                    .collect(Collectors.toList());
                            produitMap.put("images", imagesUrls);
                        } else {
                            produitMap.put("images", new ArrayList<String>());
                        }

                        // Statistiques
                        produitMap.put("nombreInteractions",
                                produit.getInteractions() != null ? produit.getInteractions().size() : 0);
                        produitMap.put("nombreCommentaires",
                                produit.getCommentaires() != null ? produit.getCommentaires().size() : 0);

                        // Date du like
                        produitMap.put("dateLike", interaction.getDate());

                        return produitMap;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Nombre de produits likés transformés: " + produitsLikes.size());
            return ResponseEntity.ok(produitsLikes);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des produits likés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }


    @GetMapping("/produits/{etat}/likes-count")
    public ResponseEntity<Map<Long, Integer>> getLikesCountByProduitEtat(@PathVariable String etat) {
        try {
            System.out.println("🎯 Récupération des likes pour les produits avec état: " + etat);

            // Récupérer tous les produits avec l'état spécifié
            List<Produit> produits = produitService.getProduitsByEtat(etat);

            // Créer une map pour stocker les compteurs de likes
            Map<Long, Integer> likesCountMap = new HashMap<>();

            // Pour chaque produit, compter les interactions
            for (Produit produit : produits) {
                int likesCount = interactionService.getInteractionCount(produit.getIdproduit());
                likesCountMap.put(produit.getIdproduit(), likesCount);
            }

            System.out.println("✅ Likes count map pour état " + etat + ": " + likesCountMap);
            return ResponseEntity.ok(likesCountMap);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des likes par état: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/vendeur/{vendeurId}/likes-count")
    public ResponseEntity<Map<Long, Integer>> getLikesCountForVendeurProduits(@PathVariable Long vendeurId) {
        try {
            System.out.println("🎯 Récupération des likes pour tous les produits du vendeur: " + vendeurId);

            // Récupérer tous les produits du vendeur
            List<Produit> produits = produitService.getProduitsByVendeurId(vendeurId);

            // Créer une map pour stocker les compteurs de likes
            Map<Long, Integer> likesCountMap = new HashMap<>();

            // Pour chaque produit, compter les interactions
            for (Produit produit : produits) {
                int likesCount = interactionService.getInteractionCount(produit.getIdproduit());
                likesCountMap.put(produit.getIdproduit(), likesCount);
            }

            System.out.println("✅ Likes count map pour vendeur " + vendeurId + ": " + likesCountMap.size() + " produits");
            return ResponseEntity.ok(likesCountMap);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des likes par vendeur: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/acheteur/{acheteurId}/likes-count")
    public ResponseEntity<Map<Long, Integer>> getLikesCountForAcheteurProduits(@PathVariable Long acheteurId) {
        try {
            System.out.println("🎯 Récupération des likes pour les produits gagnés par: " + acheteurId);

            // Récupérer tous les produits gagnés par l'acheteur
            List<Produit> produits = produitService.getProduitsGagnesByAcheteurId(acheteurId);

            // Créer une map pour stocker les compteurs de likes
            Map<Long, Integer> likesCountMap = new HashMap<>();

            // Pour chaque produit, compter les interactions
            for (Produit produit : produits) {
                int likesCount = interactionService.getInteractionCount(produit.getIdproduit());
                likesCountMap.put(produit.getIdproduit(), likesCount);
            }

            System.out.println("✅ Likes count map pour acheteur " + acheteurId + ": " + likesCountMap.size() + " produits");
            return ResponseEntity.ok(likesCountMap);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des likes par acheteur: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}