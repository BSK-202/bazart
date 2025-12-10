package com.marketplace.Enchere.controller;


import com.marketplace.Enchere.dto.EnchereDTO;
import com.marketplace.Enchere.service.EnchereService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/encheres")
@CrossOrigin(origins = "*")
public class EnchereController {

    @Autowired
    private EnchereService enchereService;

    @Autowired
    private ClientService clientService;

    // Placer une enchère
    @PostMapping("/produit/{produitId}/client/{clientId}")
    public ResponseEntity<?> placerEnchere(
            @PathVariable Long produitId,
            @PathVariable Long clientId,
            @RequestBody Map<String, Double> request) {

        try {
            Double montant = request.get("montant");

            if (montant == null) {
                return ResponseEntity.badRequest().body("Le montant est requis");
            }

            EnchereDTO enchere = enchereService.placerEnchere(produitId, clientId, montant);
            return ResponseEntity.ok(enchere);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Erreur lors du placement de l'enchère")
            );
        }
    }

    // Obtenir l'historique des enchères d'un produit
    @GetMapping("/produit/{produitId}/historique")
    public ResponseEntity<List<EnchereDTO>> getHistoriqueEncheres(@PathVariable Long produitId) {
        try {
            List<EnchereDTO> historique = enchereService.getHistoriqueEncheres(produitId);
            return ResponseEntity.ok(historique);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Obtenir l'enchère actuelle d'un produit
    @GetMapping("/produit/{produitId}/actuelle")
    public ResponseEntity<?> getEnchereActuelle(@PathVariable Long produitId) {
        try {
            return enchereService.getEnchereActuelle(produitId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Obtenir le montant actuel d'un produit
    @GetMapping("/produit/{produitId}/montant-actuel")
    public ResponseEntity<Map<String, Double>> getMontantActuel(@PathVariable Long produitId) {
        try {
            Double montant = enchereService.getMontantActuel(produitId);
            Map<String, Double> response = new HashMap<>();
            response.put("montantActuel", montant);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Obtenir les enchères d'un client
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<EnchereDTO>> getEncheresByClient(@PathVariable Long clientId) {
        try {
            List<EnchereDTO> encheres = enchereService.getEncheresByClient(clientId);
            return ResponseEntity.ok(encheres);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Obtenir le nombre d'enchères pour un produit
    @GetMapping("/produit/{produitId}/count")
    public ResponseEntity<Map<String, Long>> countEncheresByProduit(@PathVariable Long produitId) {
        try {
            // Vous devrez ajouter cette méthode dans le repository et service
            // Pour l'instant, on utilise l'historique
            List<EnchereDTO> historique = enchereService.getHistoriqueEncheres(produitId);
            Map<String, Long> response = new HashMap<>();
            response.put("count", (long) historique.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Obtenir les enchères où le client est en tête avec les montants bloqués
    @GetMapping("/client/{clientId}/leading")
    public ResponseEntity<?> getLeadingEncheresWithBlockedAmounts(@PathVariable Long clientId) {
        try {
            Map<String, Object> result = enchereService.getLeadingEncheresWithBlockedAmounts(clientId);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Erreur lors de la récupération des enchères en tête")
            );
        }
    }

    // Version pour l'utilisateur authentifié
    @GetMapping("/my-leading-encheres")
    public ResponseEntity<?> getMyLeadingEncheres(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Non authentifié"));
            }

            // Récupérer l'email depuis l'authentication
            String email = authentication.getName();
            Client client = clientService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'email: " + email));

            Map<String, Object> result = enchereService.getLeadingEncheresWithBlockedAmounts(client.getIdclient());
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Erreur lors de la récupération des enchères en tête")
            );
        }
    }

}