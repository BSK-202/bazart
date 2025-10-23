package com.marketplace.catalog.controller;

import com.marketplace.catalog.dto.ProduitDTO;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.entity.Categorie;
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
    @Autowired
    private ImageVerificationService imageVerificationService;



    // 📁 DOSSIER DE STOCKAGE (en dehors du projet frontend)
    private final String UPLOAD_DIR = "assets/produits/";

    public ProduitController(ProduitService produitService,
                             CategorieService categorieService,
                             ClientService clientService) {
        this.produitService = produitService;
        this.categorieService = categorieService;
        this.clientService = clientService;
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
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Produit créé avec succès ! En attente de validation.",
                    "produit", convertToDTO(finalProduit)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur interne : " + e.getMessage()
            ));
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

        response.setImages(produit.getImages() != null ?
                produit.getImages().stream().map(ProduitImage::getUrl).collect(Collectors.toList())
                : null);

        return response;
    }
}