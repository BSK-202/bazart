package com.marketplace.catalog.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.dto.DomaineDTO;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.service.CategorieService;
import com.marketplace.catalog.service.DomaineService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/domaines")
@CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = "*")
public class DomaineController {

    private final DomaineService domaineService;
    private final CategorieService categorieService;

    // Image storage directory
    private final String UPLOAD_DIR = "backend/assets/domaines/";
    private final String UPLOAD_DIR_CATEGORIE = "backend/assets/categories/";

    public DomaineController(DomaineService domaineService, CategorieService categorieService) {
        this.domaineService = domaineService;
        this.categorieService = categorieService;
    }
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getDomaineImage(@PathVariable String fileName) {
        System.out.println("Image");
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
            Resource resource = new UrlResource(imagePath.toUri());
            System.out.println(imagePath);

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
                System.err.println("❌ Domain image not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Error reading domain image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    @GetMapping
    public List<Domaine> getDomaines() {
        return domaineService.getDomaines();
    }

    @PostMapping(value = "/create-with-categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDomaineWithCategories(
            @RequestParam("nomDomaine") String nomDomaine,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("categories") String categoriesJson,
            @RequestParam(value = "categorieImages", required = false) MultipartFile[] categorieImages) {

        try {

            // 1. Créer et sauvegarder le domaine
            Domaine domaine = new Domaine();
            domaine.setNomDomaine(nomDomaine);
            domaine.setDescription(description);

            // Gérer l'image du domaine
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(filePath.getParent());
                imageFile.transferTo(filePath);
                domaine.setImage(fileName);
                System.out.println(" Image domaine sauvegardée: " + fileName);
            }

            Domaine savedDomaine = domaineService.save(domaine);
            System.out.println(" Domaine créé avec ID: " + savedDomaine.getIdDomaine());

            // 2. Parser et créer les catégories
            ObjectMapper mapper = new ObjectMapper();
            List<DomaineDTO.CategorieRequest> categories = mapper.readValue(
                    categoriesJson, new TypeReference<List<DomaineDTO.CategorieRequest>>() {}
            );

            System.out.println(" Nombre de catégories à créer: " + categories.size());

            for (int i = 0; i < categories.size(); i++) {
                DomaineDTO.CategorieRequest catReq = categories.get(i);
                Categorie categorie = new Categorie();
                categorie.setNomCategorie(catReq.getNomCategorie());
                categorie.setDescription(catReq.getDescription());
                categorie.setDomaine(savedDomaine);

                System.out.println(" Création catégorie " + (i + 1) + ": " + catReq.getNomCategorie());

                // Gérer l'image de catégorie
                if (categorieImages != null && i < categorieImages.length) {
                    MultipartFile catImageFile = categorieImages[i]; // CORRECTION: [i] au lieu de .get(i)
                    if (catImageFile != null && !catImageFile.isEmpty() && catImageFile.getSize() > 0) {
                        String catFileName = System.currentTimeMillis() + "_cat_" + i + "_" + catImageFile.getOriginalFilename();
                        Path catFilePath = Paths.get(UPLOAD_DIR_CATEGORIE + catFileName);
                        Files.createDirectories(catFilePath.getParent());
                        catImageFile.transferTo(catFilePath);
                        categorie.setImage(catFileName);
                        System.out.println(" Image catégorie " + (i + 1) + " sauvegardée: " + catFileName);
                    } else {
                        // Si pas d'image fournie ou fichier vide, utiliser le nom du fichier du JSON
                        categorie.setImage(catReq.getImage() != null && !catReq.getImage().isEmpty() ? catReq.getImage() : null);
                        System.out.println("ℹ Image catégorie " + (i + 1) + " depuis JSON: " + catReq.getImage());
                    }
                } else {
                    // Si pas d'image dans la liste, utiliser le nom du fichier du JSON
                    categorie.setImage(catReq.getImage() != null && !catReq.getImage().isEmpty() ? catReq.getImage() : null);
                    System.out.println("ℹ Image catégorie " + (i + 1) + " depuis JSON (pas d'image uploadée): " + catReq.getImage());
                }

                Categorie savedCategorie = categorieService.addCategorie(categorie);
                System.out.println(" Catégorie créée avec ID: " + savedCategorie.getIdCategorie());
            }

            System.out.println(" Domaine et " + categories.size() + " catégorie(s) créés avec succès!");
            return ResponseEntity.ok(savedDomaine);

        } catch (Exception e) {
            System.err.println(" Erreur lors de la création du domaine: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création du domaine: " + e.getMessage());
        }
    }

    // Endpoint pour mettre à jour un domaine
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDomaine(
            @PathVariable Long id,
            @RequestParam("nomDomaine") String nomDomaine,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        try {
            Optional<Domaine> optionalDomaine = domaineService.getDomaineById(id);
            if (optionalDomaine.isEmpty()) {
                return ResponseEntity.status(404).body("Domaine non trouvé");
            }

            Domaine domaine = optionalDomaine.get();
            domaine.setNomDomaine(nomDomaine);
            domaine.setDescription(description);

            // Gérer la nouvelle image si fournie
            if (imageFile != null && !imageFile.isEmpty()) {
                // Supprimer l'ancienne image si elle existe
                if (domaine.getImage() != null && !domaine.getImage().trim().isEmpty()) {
                    deleteDomaineImage(domaine.getImage());
                }

                // Sauvegarder la nouvelle image
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(filePath.getParent());
                imageFile.transferTo(filePath);
                domaine.setImage(fileName);
            }

            Domaine updatedDomaine = domaineService.save(domaine);
            return ResponseEntity.ok(updatedDomaine);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour du domaine: " + e.getMessage());
        }
    }

    // Endpoint pour récupérer un domaine par ID
    @GetMapping("/{id}")
    public ResponseEntity<Domaine> getDomaineById(@PathVariable Long id) {
        Optional<Domaine> domaine = domaineService.getDomaineById(id);
        return domaine.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Méthode pour supprimer l'image du domaine
    private void deleteDomaineImage(String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                System.out.println(" Image domaine supprimée: " + fileName);
            }
        } catch (Exception e) {
            System.err.println(" Erreur suppression image domaine: " + e.getMessage());
        }
    }
}