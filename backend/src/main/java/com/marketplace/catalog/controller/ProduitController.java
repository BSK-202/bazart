package com.marketplace.catalog.controller;

import com.marketplace.admin.entity.Admin;
import com.marketplace.admin.service.AdminService;
import com.marketplace.catalog.dto.ProduitDTO;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.catalog.service.ImageVerificationService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Client;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.catalog.service.CategorieService;
import com.marketplace.user.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produits")
@CrossOrigin(origins = "*")
public class ProduitController {

    private final ProduitService produitService;
    private final CategorieService categorieService;
    private final ClientService clientService;
    private final ProduitRepository produitRepository; //  DÉCLARÉ
    private final NotificationService notificationService;
    private final AdminService adminService;
    /*
    @Autowired
    private ImageVerificationService imageVerificationService;
    */

    // 📁 DOSSIER DE STOCKAGE (en dehors du projet frontend)
    private final String UPLOAD_DIR = "backend/assets/produits/";

    public ProduitController(ProduitService produitService,
                             CategorieService categorieService,
                             ClientService clientService,
                             ProduitRepository produitRepository,
                             NotificationService notificationService,
                             AdminService adminService) {
        this.produitService = produitService;
        this.categorieService = categorieService;
        this.clientService = clientService;
        this.produitRepository = produitRepository;
        this.notificationService = notificationService;
        this.adminService = adminService;
    }

    // 🆕 ENDPOINT POUR SERVIR LES IMAGES
    @GetMapping("/images/{produitId}/{fileName}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long produitId,
            @PathVariable String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + produitId + "/" + fileName);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                        // 🚫 AUCUN CACHE
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la lecture de l'image: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}")
    public ProduitDTO getProduitById(@PathVariable Long id) {
        Produit produit = produitService.getProduitById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé"));
        return convertToDTO(produit);
    }

    @GetMapping("/categorie/{idCategorie}/acceptes")
    public List<ProduitDTO> getProduitsAcceptesByCategorie(@PathVariable Long idCategorie) {
        System.out.println("🔍 Recherche produits catégorie: " + idCategorie);
        List<Produit> produits = produitService.getProduitsAcceptesByCategorie(idCategorie);
        System.out.println("📦 Nombre de produits acceptés trouvés: " + produits.size());
        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/categorie/{idCategorie}/all")
    public List<ProduitDTO> getAllProduitsByCategorie(@PathVariable Long idCategorie) {
        List<Produit> produits = produitService.getProduitsByCategorie(idCategorie);
        System.out.println("📦 Tous les produits de la catégorie " + idCategorie + ": " + produits.size());
        produits.forEach(p -> {
            System.out.println("Produit: " + p.getNom() + " - État: " + p.getEtat());
        });
        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduit(
            @RequestPart("produit") ProduitDTO produitRequest,
            @RequestPart("images") List<MultipartFile> images) {

        try {
            System.out.println("🚀 [BACKEND] Début création produit...");

            // --- Validation des champs obligatoires ---
            if (produitRequest.getCategorieId() == null)
                throw new IllegalArgumentException("L'ID de la catégorie est requis.");
            if (produitRequest.getVendeurId() == null)
                throw new IllegalArgumentException("L'ID du vendeur est requis.");
            if (images == null || images.isEmpty())
                throw new IllegalArgumentException("Aucune image n’a été fournie.");

            // --- Vérification catégorie ---
            Categorie categorie = categorieService.getCategorieById(produitRequest.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec ID: " + produitRequest.getCategorieId()));

            // --- Vérification vendeur ---
            Client vendeur = clientService.getClientById(produitRequest.getVendeurId())
                    .orElseThrow(() -> new RuntimeException("Vendeur non trouvé avec ID: " + produitRequest.getVendeurId()));

            // --- Création du produit ---
            Produit produit = new Produit();
            produit.setNom(produitRequest.getNom());
            produit.setDescription(produitRequest.getDescription());
            produit.setPrixDebut(produitRequest.getPrixDebut());
            produit.setPrixFin(produitRequest.getPrixFin());
            produit.setAExpertise(produitRequest.isAExpertise());
            produit.setDatePublication(LocalDateTime.now());
            produit.setEtat("en_attente");
            produit.setCategorie(categorie);
            produit.setVendeur(vendeur);

            Produit savedProduit = produitService.saveProduit(produit);
            Long produitId = savedProduit.getIdproduit();
            System.out.println("🎉 Produit créé avec ID: " + produitId);

            // --- Création dossier d’upload ---
            Path produitFolderPath = Paths.get(UPLOAD_DIR + produitId);
            Files.createDirectories(produitFolderPath);

            List<ProduitImage> produitImages = new ArrayList<>();
            int imageIndex = 1;
            for (MultipartFile file : images) {
                String fileExtension = getFileExtension(file.getOriginalFilename());
                String fileName = "image_" + imageIndex + fileExtension;
                Path imagePath = produitFolderPath.resolve(fileName);
                savedProduit.setImages(produitImages);
                Produit finalProduit = produitService.saveProduit(savedProduit);

                Files.write(imagePath, file.getBytes());
                ProduitImage produitImage = new ProduitImage();
                produitImage.setUrl(fileName);
                produitImage.setProduit(savedProduit);
                produitImages.add(produitImage);
                imageIndex++;
            }

            savedProduit.setImages(produitImages);
            Produit finalProduit = produitService.saveProduit(savedProduit);

            System.out.println("✅ Produit final créé avec " + produitImages.size() + " images.");

            // === NOTIFICATION: Produit créé (pour le vendeur) ===
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("productName", finalProduit.getNom());
            notifData.put("message", "Votre produit a été créé et est en attente de validation.");
            Set<Long> recipients = Set.of(finalProduit.getVendeur().getIdclient());
            notificationService.processEvent(
                    NotificationType.MESSAGE,
                    recipients,
                    notifData
            );

            // === NOTIFICATION: Produit en attente de validation (pour tous les admins) ===
            // Récupère tous les admins
            List<Admin> allAdmins = adminService.getAllAdmins();
            Set<Long> adminIds = allAdmins.stream()
                .map(Admin::getId)
                .collect(Collectors.toSet());

            Map<String, Object> adminNotifData = new HashMap<>();
            adminNotifData.put("productName", finalProduit.getNom());
            adminNotifData.put("message", "Un nouveau produit est en attente de validation.");
            adminNotifData.put("vendeurName", vendeur.getPrenom() + " " + vendeur.getNom());

            if (!adminIds.isEmpty()) {
                notificationService.processEvent(
                    NotificationType.ADMIN_ALERT,
                    adminIds,
                    adminNotifData
                );
            }

            return ResponseEntity.ok(convertToDTO(finalProduit));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création du produit: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public ProduitDTO convertToDTO(Produit produit) {
        ProduitDTO response = new ProduitDTO();
        response.setId(produit.getIdproduit());
        response.setNom(produit.getNom());
        response.setDescription(produit.getDescription());
        response.setPrixDebut(produit.getPrixDebut());
        response.setPrixFin(produit.getPrixFin());
        response.setEtat(produit.getEtat());

        response.setVendeurNom(produit.getVendeur() != null ? produit.getVendeur().getNom() : null);
        response.setAcheteurNom(produit.getAcheteur() != null ? produit.getAcheteur().getNom() : null);
        response.setCategorieNom(produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : null);

        response.setNombreInteractions(produit.getInteractions() != null ? produit.getInteractions().size() : 0);
        response.setNombreCommentaires(produit.getCommentaires() != null ? produit.getCommentaires().size() : 0);
        // Ajouter la date de publication
        // ✅ CORRECTION: Utiliser camelCase partout
        if (produit.getDatePublication() != null) {
            response.setDatepublication(produit.getDatePublication().toString());
            System.out.println("📅 Date de publication convertie: " + produit.getDatePublication().toString());
        } else {
            System.out.println("⚠️ Date de publication est null");
            response.setDatepublication(LocalDateTime.now().toString());
        }

        // ✅ NOUVEAU : Date d'enchère
        if (produit.getDateEnchere() != null) {
            response.setDateenchere(produit.getDateEnchere().toString());
        }
        response.setVendeurId(produit.getVendeur() != null ? produit.getVendeur().getIdclient() : null);
        response.setImages(produit.getImages() != null ?
                produit.getImages().stream().map(ProduitImage::getUrl).collect(Collectors.toList())
                : null);

        return response;
    }

    // ENDPOINTS POUR LES PRODUITS EN ATTENTE
    @GetMapping("/en-attente")
    public ResponseEntity<List<ProduitDTO>> getProduitsEnAttente() {
        try {
            System.out.println(" Recherche des produits en attente...");
            List<Produit> produits = produitService.getProduitsEnAttente();
            System.out.println(" Nombre de produits en attente trouvés: " + produits.size());

            List<ProduitDTO> produitsDTO = produits.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(produitsDTO);
        } catch (Exception e) {
            System.err.println(" Erreur lors de la récupération des produits en attente: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/en-attente/count")
    public ResponseEntity<Map<String, Long>> countProduitsEnAttente() {
        try {
            System.out.println(" Calcul du nombre de produits en attente...");
            long count = produitService.countProduitsEnAttente();
            System.out.println("Nombre de produits en attente: " + count);

            Map<String, Long> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println(" Erreur lors du comptage des produits en attente: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // ENDPOINT POUR RÉCUPÉRER TOUS LES PRODUITS AVEC FILTRE ÉTAT (optionnel)
    @GetMapping("/etat/{etat}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByEtat(@PathVariable String etat) {
        try {
            System.out.println(" Recherche des produits avec état: " + etat);

            List<Produit> produits = produitRepository.findByEtat(etat);

            System.out.println(" Nombre de produits avec état '" + etat + "': " + produits.size());

            List<ProduitDTO> produitsDTO = produits.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(produitsDTO);
        } catch (Exception e) {
            System.err.println(" Erreur lors de la récupération des produits par état: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // ENDPOINT POUR METTRE À JOUR L'ÉTAT DU PRODUIT
    @PutMapping("/{id}/etat")
    public ResponseEntity<?> updateProductState(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            String newState = request.get("etat");
            String noteAdmin = request.get("noteAdmin");

            System.out.println("🔄 Mise à jour état produit ID: " + id + " -> " + newState);

            Produit produit = produitService.getProduitById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec ID: " + id));

            produit.setEtat(newState);

            Produit updatedProduit = produitService.saveProduit(produit);

            // === NOTIFICATION LOGIC STARTS HERE ===
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("productName", updatedProduit.getNom());
            if (noteAdmin != null && !noteAdmin.trim().isEmpty()) {
                notifData.put("adminMessage", noteAdmin);
            }

            Set<Long> recipients = Set.of(updatedProduit.getVendeur().getIdclient());

            NotificationType notifType;
            if ("accepte".equalsIgnoreCase(newState) || "Accepté".equalsIgnoreCase(newState)) {
                notifType = NotificationType.PRODUCT_ACCEPTED;
            } else if ("refuse".equalsIgnoreCase(newState) || "Refusé".equalsIgnoreCase(newState)) {
                notifType = NotificationType.PRODUCT_REFUSED;
            } else {
                notifType = NotificationType.GENERIC;
            }

            notificationService.processEvent(
                notifType,
                recipients,
                notifData
            );
            // === NOTIFICATION LOGIC ENDS HERE ===
            System.out.println("✅ État produit mis à jour: " + updatedProduit.getEtat());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produit " + (newState.equals("accepte") ? "accepté" : "refusé") + " avec succès"
            ));

        } catch (Exception e) {
            System.err.println("❌ Erreur mise à jour état produit: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour du produit"
            ));
        }
    }

    // ENDPOINT POUR RÉCUPÉRER LES PRODUITS DU CLIENT CONNECTÉ
    @GetMapping("/vendeur/{vendeurId}")
    public List<ProduitDTO> getProduitsByVendeur(@PathVariable Long vendeurId) {
        System.out.println("🔍 Recherche produits du vendeur: " + vendeurId);
        List<Produit> produits = produitService.getProduitsByVendeur(vendeurId);
        System.out.println("📦 Nombre de produits trouvés: " + produits.size());

        produits.forEach(p -> {
            System.out.println("Produit: " + p.getNom() + " - État: " + p.getEtat());
        });

        return produits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ENDPOINT POUR DÉMARRER UNE ENCHÈRE
    @PostMapping("/{produitId}/start-auction")
    public ResponseEntity<?> startAuction(@PathVariable Long produitId) {
        try {
            System.out.println("🚀 Démarrage de l'enchère pour le produit: " + produitId);

            Produit produit = produitService.getProduitById(produitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé"));

            // Vérifier que le produit est dans un état qui permet de démarrer une enchère
            if (!"accepter".equals(produit.getEtat()) && !"accepte".equals(produit.getEtat())) {
                return ResponseEntity.badRequest().body("Le produit doit être accepté pour démarrer une enchère. État actuel: " + produit.getEtat());
            }

            // Changer l'état à "en_enchere"
            produit.setEtat("en_enchere");

            // ✅ NOUVEAU : Définir la date de début d'enchère
            produit.setDateEnchere(LocalDateTime.now());

            Produit updatedProduit = produitService.saveProduit(produit);

            // === NOTIFICATION: Enchère démarrée ===
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("productName", updatedProduit.getNom());
            notifData.put("message", "Enchère démarrée pour votre produit.");
            Set<Long> recipients = Set.of(updatedProduit.getVendeur().getIdclient());
            notificationService.processEvent(
                    NotificationType.AUCTION_START,
                    recipients,
                    notifData
            );

            System.out.println("✅ Enchère démarrée avec succès pour le produit: " + produitId);
            System.out.println("📅 Date d'enchère définie: " + updatedProduit.getDateEnchere());

            return ResponseEntity.ok(convertToDTO(updatedProduit));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l'enchère: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors du démarrage de l'enchère: " + e.getMessage());
        }
    }
}