package com.marketplace.catalog.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.dto.ProduitDTO;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.catalog.service.ImageVerificationService;
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

    /*
    @Autowired
    private ImageVerificationService imageVerificationService;
*/


    // 📁 DOSSIER DE STOCKAGE (en dehors du projet frontend)
    private final String UPLOAD_DIR = "backend/assets/produits/";

    public ProduitController(ProduitService produitService,
                             CategorieService categorieService,
                             ClientService clientService, ProduitRepository produitRepository) {
        this.produitService = produitService;
        this.categorieService = categorieService;
        this.clientService = clientService;
        this.produitRepository = produitRepository;
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

              /*
            for (MultipartFile file : images) {


                // Vérification authenticité
                boolean estAuthentique = imageVerificationService.verifierImageAuthentique(file);
                if (!estAuthentique) {
                    System.err.println("🚫 Image rejetée : IA détectée ou confiance < 99%");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "success", false,
                                    "message", "Une ou plusieurs images ont été rejetées : non authentiques."
                            ));
                }
            }
              */

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
            return ResponseEntity.ok( convertToDTO(finalProduit));

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
            response.setDatepublication(produit.getDatePublication().toString()); // ✅ camelCase
            System.out.println("📅 Date de publication convertie: " + produit.getDatePublication().toString());
        } else {
            System.out.println("⚠️ Date de publication est null");
            response.setDatepublication(LocalDateTime.now().toString()); // ✅ camelCase
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
    //  ENDPOINTS POUR LES PRODUITS EN ATTENTE
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

    //  ENDPOINT POUR RÉCUPÉRER TOUS LES PRODUITS AVEC FILTRE ÉTAT (optionnel)
    @GetMapping("/etat/{etat}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByEtat(@PathVariable String etat) {
        try {
            System.out.println(" Recherche des produits avec état: " + etat);

            // MAINTENANT produitRepository EST INITIALISÉ
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

            // Si vous avez un champ pour stocker la note d'admin, vous pouvez l'ajouter ici
            if (noteAdmin != null && !noteAdmin.trim().isEmpty()) {
                System.out.println("📝 Note admin: " + noteAdmin);
                // produit.setNoteAdmin(noteAdmin); // Décommentez si vous avez ce champ
            }

            Produit updatedProduit = produitService.saveProduit(produit);

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
    // 🆕 ENDPOINT POUR RÉCUPÉRER LES PRODUITS DU CLIENT CONNECTÉ
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

    // 🆕 ENDPOINT POUR DÉMARRER UNE ENCHÈRE
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

            System.out.println("✅ Enchère démarrée avec succès pour le produit: " + produitId);
            System.out.println("📅 Date d'enchère définie: " + updatedProduit.getDateEnchere());

            return ResponseEntity.ok(convertToDTO(updatedProduit));

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l'enchère: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors du démarrage de l'enchère: " + e.getMessage());
        }
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

            // 🗑️ SUPPRIMER SEULEMENT LES IMAGES MARQUÉES POUR SUPPRESSION
            if (imagesToDeleteJson != null && !imagesToDeleteJson.trim().isEmpty()) {
                try {
                    List<String> imagesToDelete = new ObjectMapper().readValue(imagesToDeleteJson, new TypeReference<List<String>>() {});
                    System.out.println("🗑️ Images à supprimer: " + imagesToDelete);

                    for (String imageName : imagesToDelete) {
                        // Supprimer du système de fichiers
                        Path imagePath = produitFolderPath.resolve(imageName);
                        if (Files.exists(imagePath)) {
                            Files.delete(imagePath);
                            System.out.println("✅ Image supprimée du dossier: " + imageName);
                        }

                        // Supprimer de la base de données
                        boolean removed = produit.getImages().removeIf(img -> imageName.equals(img.getUrl()));
                        if (removed) {
                            System.out.println("✅ Image supprimée de la base: " + imageName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur lors du traitement des images à supprimer: " + e.getMessage());
                }
            }

            // 📷 AJOUTER LES NOUVELLES IMAGES (sans supprimer les existantes)
            if (images != null && !images.isEmpty()) {
                System.out.println("🖼️ Ajout de " + images.size() + " nouvelles images pour le produit ID: " + id);

                // Trouver le prochain index disponible
                int nextIndex = 1;
                if (!produit.getImages().isEmpty()) {
                    nextIndex = produit.getImages().size() + 1;
                }

                // Ajouter les nouvelles images
                for (int i = 0; i < images.size(); i++) {
                    MultipartFile file = images.get(i);
                    String fileExtension = getFileExtension(file.getOriginalFilename());
                    String fileName = "image_" + (nextIndex + i) + fileExtension;
                    Path imagePath = produitFolderPath.resolve(fileName);

                    // Vérifier si le fichier existe déjà (au cas où)
                    if (!Files.exists(imagePath)) {
                        Files.write(imagePath, file.getBytes());
                        System.out.println("✅ Nouvelle image sauvegardée: " + fileName);

                        ProduitImage produitImage = new ProduitImage();
                        produitImage.setUrl(fileName);
                        produitImage.setProduit(produit);
                        produit.getImages().add(produitImage);
                    } else {
                        System.err.println("⚠️ Image déjà existante, ignorée: " + fileName);
                    }
                }
            }

            System.out.println("📊 Total images après mise à jour: " + produit.getImages().size());

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
}
