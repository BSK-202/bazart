// CommentaireController.java - VERSION CORRIGÉE
package com.marketplace.interaction.controller;

import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.service.CommentaireService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/commentaires")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentaireController {

    @Autowired
    private CommentaireService commentaireService;

    @Autowired
    private ClientRepository clientRepository;

    private Long getCurrentClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔐 Authentication object: " + authentication);
        System.out.println("🔐 Is authenticated: " + (authentication != null && authentication.isAuthenticated()));
        System.out.println("🔐 Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
        System.out.println("🔐 Name: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("🔐 Authorities: " + (authentication != null ? authentication.getAuthorities() : "null"));

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            System.out.println("🔐 Username from authentication: " + username);

            // Vérifier si l'utilisateur est anonyme
            if ("anonymousUser".equals(username)) {
                System.out.println("❌ User is anonymous - JWT filter not working");
                throw new RuntimeException("Utilisateur non authentifié - filtre JWT non fonctionnel");
            }

            // Rechercher le client par email
            Optional<Client> clientOpt = clientRepository.findByEmail(username);
            if (clientOpt.isPresent()) {
                Long clientId = clientOpt.get().getIdclient();
                System.out.println("✅ Client ID trouvé: " + clientId);
                return clientId;
            } else {
                System.out.println("❌ Aucun client trouvé avec l'email: " + username);
                throw new RuntimeException("Client non trouvé pour l'email: " + username);
            }
        }

        System.out.println("❌ Aucune authentification trouvée");
        throw new RuntimeException("Utilisateur non authentifié");
    }

    // CommentaireController.java - LOGS DÉTAILLÉS
    @GetMapping("/produit/{produitId}")
    public List<Commentaire> getCommentairesByProduit(@PathVariable Long produitId) {
        System.out.println("🎯 API CALL: GET /api/commentaires/produit/" + produitId);

        try {
            List<Commentaire> commentaires = commentaireService.getCommentairesByProduit(produitId);

            System.out.println("📊 Nombre de commentaires trouvés: " + commentaires.size());

            // Log détaillé de chaque commentaire
            for (Commentaire c : commentaires) {
                System.out.println("📝 Commentaire ID: " + c.getIdcommentaire());
                System.out.println("   Contenu: " + c.getContenu());
                System.out.println("   Date: " + c.getDate());
                System.out.println("   Client: " + (c.getClient() != null ?
                        c.getClient().getIdclient() + " - " + c.getClient().getPrenom() + " " + c.getClient().getNom() : "NULL"));
            }

            return commentaires;
        } catch (Exception e) {
            System.out.println("❌ ERREUR CRITIQUE dans getCommentairesByProduit: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // CommentaireController.java - VERSION AMÉLIORÉE
    @PostMapping("/produit/{produitId}")
    public ResponseEntity<?> addCommentaire(
            @PathVariable Long produitId,
            @RequestBody Map<String, String> request) {
        try {
            System.out.println("📥 POST Commentaire pour produit: " + produitId);

            // ✅ Récupérer clientId depuis le token JWT
            Long clientId = getCurrentClientId();
            String contenu = request.get("contenu");

            System.out.println("🔍 Client ID: " + clientId + ", Contenu: " + contenu);

            if (contenu == null || contenu.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le contenu ne peut pas être vide");
            }

            Commentaire commentaire = commentaireService.addCommentaire(produitId, clientId, contenu.trim());

            // ✅ Retourner directement le commentaire (pas de wrapper)
            return ResponseEntity.ok(commentaire);

        } catch (Exception e) {
            System.out.println("❌ Erreur ajout commentaire: " + e.getMessage());
            e.printStackTrace();

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{commentaireId}")
    public ResponseEntity<?> deleteCommentaire(@PathVariable Long commentaireId) {
        try {
            // ✅ Récupérer clientId depuis le token
            Long clientId = getCurrentClientId();
            commentaireService.deleteCommentaire(commentaireId, clientId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/produit/{produitId}/count")
    public int getNombreCommentaires(@PathVariable Long produitId) {
        return commentaireService.getNombreCommentaires(produitId);
    }
}