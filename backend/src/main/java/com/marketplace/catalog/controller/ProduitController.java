package com.marketplace.catalog.controller;

import com.marketplace.admin.entity.Admin;
import com.marketplace.admin.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.fasterxml.jackson.core.type.TypeReference;
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
        // ✅ AJOUTER LA DURÉE D'ENCHÈRE
        response.setDureeEnchereJours(produit.getDureeEnchereJours());
        // Ajouter la date de publication
        // ✅ CORRECTION: Utiliser camelCase partout
        if (produit.getDatePublication() != null) {
            response.setDatepublication(produit.getDatePublication().toString());
            System.out.println("📅 Date de publication convertie: " + produit.getDatePublication().toString());
        } else {
            System.out.println("⚠️ Date de publication est null");
            response.setDatepublication(LocalDateTime.now().toString());
        }
        // ✅ AJOUTER CES CHAMPS POUR L'ÉDITION
        if (produit.getCategorie() != null) {
            response.setCategorieId(produit.getCategorie().getIdCategorie());
            response.setCategorieNom(produit.getCategorie().getNomCategorie());

            // ✅ AJOUTER L'ID DU DOMAINE POUR PRÉ-REMPLISSAGE
            if (produit.getCategorie().getDomaine() != null) {
                response.setDomaineId(produit.getCategorie().getDomaine().getIdDomaine());
            }
        }

        if (produit.getAcheteur() != null) {
            response.setAcheteurId(produit.getAcheteur().getIdclient());
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
// Dans ProduitController.java, modifier l'endpoint start-auction :

    @PostMapping("/{produitId}/start-auction")
    public ResponseEntity<?> startAuction(
            @PathVariable Long produitId,
            @RequestBody Map<String, Object> requestBody) {

        try {
            System.out.println("🚀 Démarrage de l'enchère pour le produit: " + produitId);

            Produit produit = produitService.getProduitById(produitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé"));

            // Vérifier que le produit est dans un état qui permet de démarrer une enchère
            if (!"accepter".equals(produit.getEtat()) && !"accepte".equals(produit.getEtat())) {
                return ResponseEntity.badRequest().body("Le produit doit être accepté pour démarrer une enchère. État actuel: " + produit.getEtat());
            }

            // ✅ RÉCUPÉRER LA DURÉE DEPUIS LA REQUÊTE
            Integer dureeEnchereJours = (Integer) requestBody.get("dureeEnchereJours");

            if (dureeEnchereJours == null || dureeEnchereJours < 1) {
                return ResponseEntity.badRequest().body("Durée d'enchère invalide");
            }

            // ✅ SAUVEGARDER LA DURÉE DANS LE PRODUIT
            produit.setDureeEnchereJours(dureeEnchereJours);

            // Changer l'état à "en_enchere"
            produit.setEtat("en_enchere");

            // ✅ DÉFINIR LA DATE DE DÉBUT D'ENCHÈRE
            produit.setDateEnchere(LocalDateTime.now());

            Produit updatedProduit = produitService.saveProduit(produit);

            // === NOTIFICATION: Enchère démarrée (pour le vendeur) ===
            Map<String, Object> vendeurNotifData = new HashMap<>();
            vendeurNotifData.put("productName", updatedProduit.getNom());
            vendeurNotifData.put("message", "Enchère démarrée pour votre produit.");
            Set<Long> vendeurRecipients = Set.of(updatedProduit.getVendeur().getIdclient());
            notificationService.processEvent(
                    NotificationType.AUCTION_START,
                    vendeurRecipients,
                    vendeurNotifData
            );

            // === NOUVELLE NOTIFICATION: Enchère démarrée (pour les utilisateurs ayant interagi) ===
            Set<Long> usersWhoInteracted = produitService.getUsersWhoInteractedWithProduct(produitId);

            if (!usersWhoInteracted.isEmpty()) {
                Map<String, Object> interactionNotifData = new HashMap<>();
                interactionNotifData.put("productName", updatedProduit.getNom());
                interactionNotifData.put("message", "Un produit que vous avez aimé ou commenté est maintenant en enchère !");
                interactionNotifData.put("productId", produitId);

                notificationService.processEvent(
                        NotificationType.AUCTION_START, // Ou créer un nouveau type si nécessaire
                        usersWhoInteracted,
                        interactionNotifData
                );

                System.out.println("📢 Notification envoyée à " + usersWhoInteracted.size() +
                        " utilisateurs ayant interagi avec le produit");
            }

            System.out.println("✅ Enchère démarrée avec succès pour le produit: " + produitId);
            System.out.println("📅 Durée de l'enchère: " + dureeEnchereJours + " jours");
            System.out.println("📅 Date d'enchère définie: " + updatedProduit.getDateEnchere());

            return ResponseEntity.ok(convertToDTO(updatedProduit));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l'enchère: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors du démarrage de l'enchère: " + e.getMessage());
        }
    }
    // AJOUTER CETTE VÉRIFICATION avant d'ajouter une nouvelle image
    private boolean isImageAlreadyExists(List<ProduitImage> existingImages, String fileName) {
        return existingImages.stream()
                .anyMatch(img -> fileName.equals(img.getUrl()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduit(
            @PathVariable Long id,
            @RequestPart("produit") ProduitDTO produitRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "imagesToDelete", required = false) String imagesToDeleteJson) {

        try {
            System.out.println("✏️ [BACKEND] Modification du produit ID: " + id);

            // 🔍 Récupérer le produit existant
            Produit produit = produitService.getProduitById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit non trouvé avec ID: " + id));

            // 🔄 Mettre à jour les champs simples
            produit.setNom(produitRequest.getNom());
            produit.setDescription(produitRequest.getDescription());
            produit.setPrixDebut(produitRequest.getPrixDebut());
            produit.setPrixFin(produitRequest.getPrixFin());
            produit.setAExpertise(produitRequest.isAExpertise());
            produit.setEtat(produitRequest.getEtat() != null ? produitRequest.getEtat() : produit.getEtat());

            // 🔗 Mettre à jour la catégorie si modifiée
            if (produitRequest.getCategorieId() != null) {
                Categorie categorie = categorieService.getCategorieById(produitRequest.getCategorieId())
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec ID: " + produitRequest.getCategorieId()));
                produit.setCategorie(categorie);
            }

            // 📁 Dossier du produit
            Path produitFolderPath = Paths.get(UPLOAD_DIR + id);
            if (!Files.exists(produitFolderPath)) {
                Files.createDirectories(produitFolderPath);
            }

            // 📷 GESTION DE LA SUPPRESSION DES IMAGES VIA LE SERVICE
            if (imagesToDeleteJson != null && !imagesToDeleteJson.trim().isEmpty()) {
                try {
                    List<String> imagesToDelete = new ObjectMapper().readValue(imagesToDeleteJson, new TypeReference<List<String>>() {});
                    System.out.println("🗑️ Images à supprimer reçues: " + imagesToDelete);

                    // Supprimer les fichiers du dossier
                    for (String imageName : imagesToDelete) {
                        Path imagePath = produitFolderPath.resolve(imageName);
                        if (Files.exists(imagePath)) {
                            Files.delete(imagePath);
                            System.out.println("✅ Image supprimée du dossier: " + imageName);
                        }
                    }

                    // Supprimer de la base de données via le service
                    produitService.deleteProduitImages(produit, imagesToDelete);

                } catch (Exception e) {
                    System.err.println("⚠️ Erreur lors du traitement des images à supprimer: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 📷 GESTION DE L'AJOUT DES NOUVELLES IMAGES VIA LE SERVICE
            if (images != null && !images.isEmpty()) {
                produitService.addProduitImages(produit, images, produitFolderPath);
            }

            System.out.println("📊 Total images après mise à jour: " + (produit.getImages() != null ? produit.getImages().size() : 0));

            // 💾 Sauvegarder le produit modifié
            Produit updatedProduit = produitService.saveProduit(produit);

            System.out.println("✅ Produit mis à jour avec succès : " + updatedProduit.getNom());
            return ResponseEntity.ok(convertToDTO(updatedProduit));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la modification du produit : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour du produit : " + e.getMessage()
            ));
        }
    }

    // 🆕 MÉTHODE POUR TROUVER LE PROCHAIN INDEX D'IMAGE DISPONIBLE
    private int findNextAvailableImageIndex(List<ProduitImage> existingImages) {
        if (existingImages == null || existingImages.isEmpty()) {
            return 1;
        }

        // Extraire tous les numéros d'images existants
        Set<Integer> existingNumbers = new HashSet<>();
        for (ProduitImage image : existingImages) {
            String url = image.getUrl();
            if (url != null && url.startsWith("image_")) {
                try {
                    // Extraire le numéro de "image_1.jpg", "image_2.png", etc.
                    String numberStr = url.substring(6, url.lastIndexOf('.'));
                    int number = Integer.parseInt(numberStr);
                    existingNumbers.add(number);
                } catch (Exception e) {
                    System.err.println("⚠️ Impossible d'extraire le numéro de l'image: " + url);
                }
            }
        }

        // Trouver le prochain numéro disponible
        int nextNumber = 1;
        while (existingNumbers.contains(nextNumber)) {
            nextNumber++;
        }

        System.out.println("🔢 Prochain index d'image disponible: " + nextNumber);
        return nextNumber;
    }

    // 🆕 AJOUTER CETTE MÉTHODE MANQUANTE
    @PostMapping("/{clientId}/photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @PathVariable Long clientId,
            @RequestParam("photoProfil") MultipartFile file) {

        try {
            System.out.println("📤 Upload de photo de profil pour le client ID: " + clientId);

            Optional<Client> clientOpt = clientService.getClientById(clientId);
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Client non trouvé");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Aucun fichier fourni");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Le fichier doit être une image");
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("L'image ne doit pas dépasser 5MB");
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = ".jpg";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            }

            String newFileName = clientId + fileExtension;
            Path filePath = Paths.get(UPLOAD_DIR + newFileName);

            // Supprimer l'ancienne image si elle existe
            Files.deleteIfExists(filePath);
            Files.copy(file.getInputStream(), filePath);

            System.out.println("✅ Photo de profil sauvegardée: " + filePath.toString());

            // Mettre à jour le client avec le nouveau nom de fichier
            Client client = clientOpt.get();
            client.setPhotoprofil(newFileName);
            clientService.updateClient(client);

            // Construire l'URL complète
            String profileImageUrl = "http://localhost:8080/api/clients/images/" + newFileName;

            Map<String, String> response = new HashMap<>();
            response.put("fileName", newFileName);
            response.put("photoProfil", profileImageUrl); // ✅ CORRECTION: utiliser "photoProfil" au lieu de "profileImageUrl"
            response.put("message", "Photo de profil uploadée avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'upload de la photo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de l'upload de la photo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }
    // ENDPOINT SIMPLE POUR TERMINER L'ENCHÈRE
    @PostMapping("/{produitId}/terminer-enchere")
    public ResponseEntity<?> terminerEnchere(
            @PathVariable Long produitId,
            @RequestBody(required = false) Map<String, Object> requestBody) {

        try {
            Produit produit = produitService.getProduitById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

            // Vérifier que le produit est en enchère
            if (!"en_enchere".equals(produit.getEtat())) {
                return ResponseEntity.badRequest().body(
                        "Le produit n'est pas en enchère. État actuel: " + produit.getEtat()
                );
            }

            // Récupérer l'ID du gagnant si fourni
            Long idGagnant = null;
            if (requestBody != null && requestBody.containsKey("idGagnant")) {
                idGagnant = ((Number) requestBody.get("idGagnant")).longValue();

                // Vérifier que le gagnant existe
                Optional<Client> gagnantOpt = clientService.getClientById(idGagnant);
                if (gagnantOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Client gagnant non trouvé avec ID: " + idGagnant);
                }

                // ✅ CORRECTION : Définir l'acheteur complet, pas juste l'ID
                produit.setAcheteur(gagnantOpt.get());

                // Optionnel : définir le prix final (dernière enchère)
                // Vous pouvez récupérer le montant de la dernière enchère ici
                // produit.setPrixFin(dernierMontant);
            }

            // Mettre à jour l'état
            produit.setEtat("enchere_termine");
            Produit updatedProduit = produitService.saveProduit(produit);

            // === NOTIFICATION: Enchère terminée ===
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("productName", updatedProduit.getNom());
            notifData.put("message", "L'enchère est terminée pour votre produit.");

            Set<Long> recipients = new HashSet<>();
            recipients.add(updatedProduit.getVendeur().getIdclient());

            // Notifier aussi le gagnant s'il y en a un
            if (idGagnant != null) {
                recipients.add(idGagnant);
                notifData.put("isWinner", true);
                notifData.put("winningAmount", updatedProduit.getPrixFin());
            }

            notificationService.processEvent(
                    NotificationType.AUCTION_END,
                    recipients,
                    notifData
            );

            System.out.println("✅ Enchère terminée pour le produit: " + produitId);
            if (idGagnant != null) {
                System.out.println("🏆 Gagnant: Client ID " + idGagnant);
                System.out.println("📊 Acheteur défini: " + updatedProduit.getAcheteur().getNom() + " " + updatedProduit.getAcheteur().getPrenom());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Enchère terminée avec succès" + (idGagnant != null ? ", gagnant enregistré" : ""),
                    "idClientAcheteur", idGagnant
            ));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la fin de l'enchère: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur: " + e.getMessage());
        }
    }
    // ENDPOINT POUR RÉCUPÉRER TOUS LES PRODUITS EN ENCHÈRE
    @GetMapping("/encheres")
    public ResponseEntity<List<ProduitDTO>> getProduitsEnEnchere() {
        try {
            System.out.println("🔍 Recherche des produits en enchère...");
            List<Produit> produits = produitService.getProduitsByEtat("en_enchere");
            System.out.println("📦 Nombre de produits en enchère trouvés: " + produits.size());

            List<ProduitDTO> produitsDTO = produits.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(produitsDTO);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des produits en enchère: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Dans ProduitController.java, ajouter cet endpoint
    @GetMapping("/acheteur/{acheteurId}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByAcheteur(@PathVariable Long acheteurId) {
        try {
            System.out.println("🔍 Recherche des produits gagnés par l'acheteur: " + acheteurId);

            List<Produit> produits = produitService.getProduitsByAcheteur(acheteurId);
            System.out.println("📦 Nombre de produits gagnés trouvés: " + produits.size());

            List<ProduitDTO> produitsDTO = produits.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(produitsDTO);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération des produits gagnés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
