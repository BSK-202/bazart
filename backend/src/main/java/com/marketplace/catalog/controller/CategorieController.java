package com.marketplace.catalog.controller;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.repository.DomaineRepository;
import com.marketplace.catalog.service.CategorieService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategorieController {

    private final CategorieService categorieService;
    private final DomaineRepository domaineRepository;

    // 📁 Dossier de stockage des images de catégories
    private final String UPLOAD_DIR = "backend/assets/categories/";

    public CategorieController(CategorieService categorieService, DomaineRepository domaineRepository) {

        this.categorieService = categorieService;
        this.domaineRepository = domaineRepository;

    }

    // 🆕 ENDPOINT pour servir les images de catégories
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getCategorieImage(@PathVariable String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists()) {
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
                System.err.println("❌ Image catégorie non trouvée: " + imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lecture image catégorie: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/domaine/{idDomaine}")
    public List<Categorie> getCategoriesByDomaine(@PathVariable Long idDomaine) {
        return categorieService.getCategoriesByDomaine(idDomaine);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getCategorieById(@PathVariable Long id) {
        Optional<Categorie> categorie = categorieService.getCategorieById(id);
        return categorie.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // CategorieController.java

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Categorie> createCategorie(
            @RequestParam("nomCategorie") String nomCategorie,
            @RequestParam("description") String description,
            @RequestParam("idDomaine") Long idDomaine,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            // Récupérer le domaine existant via l'instance injectée
            Optional<Domaine> optionalDomaine = domaineRepository.findById(idDomaine);
            if (optionalDomaine.isEmpty()) {
                return ResponseEntity.status(404).build(); // Domaine non trouvé
            }

            Domaine domaine = optionalDomaine.get();

            // Créer la catégorie
            Categorie categorie = new Categorie();
            categorie.setNomCategorie(nomCategorie);
            categorie.setDescription(description);
            categorie.setDomaine(domaine); // Associer domaine existant

            // Gérer l'image si elle existe
            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(filePath.getParent());
                imageFile.transferTo(filePath);
                categorie.setImage(fileName);
            }

            // Sauvegarder la catégorie
            Categorie saved = categorieService.addCategorie(categorie);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }






    // Endpoint pour mettre à jour une catégorie avec image
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCategorieWithImage(
            @PathVariable Long id,
            @RequestParam("nomCategorie") String nomCategorie,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        try {
            Optional<Categorie> optionalCategorie = categorieService.getCategorieById(id);
            if (optionalCategorie.isEmpty()) {
                return ResponseEntity.status(404).body("Catégorie non trouvée");
            }

            Categorie categorie = optionalCategorie.get();
            categorie.setNomCategorie(nomCategorie);
            categorie.setDescription(description);

            // Gérer la nouvelle image si fournie
            if (imageFile != null && !imageFile.isEmpty()) {
                // Supprimer l'ancienne image si elle existe
                if (categorie.getImage() != null && !categorie.getImage().trim().isEmpty()) {
                    deleteCategorieImage(categorie.getImage());
                }

                // Sauvegarder la nouvelle image
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(filePath.getParent());
                imageFile.transferTo(filePath);
                categorie.setImage(fileName);
            }

            Categorie updatedCategorie = categorieService.updateCategorie(id, categorie);
            return ResponseEntity.ok(updatedCategorie);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la mise à jour de la catégorie: " + e.getMessage());
        }
    }

    // Gardez l'ancienne méthode PUT pour la compatibilité (ou supprimez-la si vous voulez seulement la nouvelle)
    @PutMapping("/{id}")
    public ResponseEntity<Categorie> updateCategorie(
            @PathVariable Long id,
            @RequestBody Categorie categorie
    ) {
        try {
            Categorie updated = categorieService.updateCategorie(id, categorie);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    /*
      @DeleteMapping("/{id}")
      public ResponseEntity<String> deleteCategorie(@PathVariable Long id) {
          try {
              // Vérifier si la catégorie existe
              Optional<Categorie> optionalCategorie = categorieService.getCategorieById(id);
              if (optionalCategorie.isEmpty()) {
                  return ResponseEntity.status(404).body("Catégorie non trouvée");
              }

              Categorie categorie = optionalCategorie.get();
              Domaine domaine = categorie.getDomaine();
              Long idDomaine = domaine.getIdDomaine();

              // Sauvegarder le nom de l'image avant suppression
              String imageFileName = categorie.getImage();

              // Compter le nombre de catégories dans le domaine
              List<Categorie> categoriesDuDomaine = categorieService.getCategoriesByDomaine(idDomaine);
              boolean isLastCategory = categoriesDuDomaine.size() == 1;

              // Supprimer la catégorie
              boolean deleted = categorieService.deleteCategorie(id);

              if (deleted) {
                  // Supprimer l'image physique si elle existe
                  if (imageFileName != null && !imageFileName.trim().isEmpty()) {
                      deleteCategorieImage(imageFileName);
                  }

                  // Supprimer le domaine si c'était la dernière catégorie
                  if (isLastCategory) {
                      domaineRepository.delete(domaine);
                      // Supprimer aussi l'image du domaine
                      if (domaine.getImage() != null && !domaine.getImage().trim().isEmpty()) {
                          deleteDomaineImage(domaine.getImage());
                      }
                      return ResponseEntity.ok("Catégorie et domaine supprimés avec succès (c'était la dernière catégorie)");
                  }

                  return ResponseEntity.ok("Catégorie supprimée avec succès");
              } else {
                  return ResponseEntity.status(404).body("Catégorie non trouvée");
              }

          } catch (org.springframework.dao.DataIntegrityViolationException e) {
              return ResponseEntity.status(400).body(
                      "Impossible de supprimer cette catégorie car elle contient encore des produits associés."
              );
          } catch (Exception e) {
              e.printStackTrace();
              return ResponseEntity.status(500).body("Erreur interne du serveur lors de la suppression de la catégorie.");
          }
      }*/
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategorie(@PathVariable Long id) {
        try {
            // Vérifier si la catégorie existe
            Optional<Categorie> optionalCategorie = categorieService.getCategorieById(id);
            if (optionalCategorie.isEmpty()) {
                Map<String, String> response = Map.of("message", "Catégorie non trouvée");
                return ResponseEntity.status(404).body(response);
            }

            Categorie categorie = optionalCategorie.get();
            Domaine domaine = categorie.getDomaine();
            Long idDomaine = domaine.getIdDomaine();

            // Sauvegarder le nom de l'image avant suppression
            String imageFileName = categorie.getImage();

            // Compter le nombre de catégories dans le domaine
            List<Categorie> categoriesDuDomaine = categorieService.getCategoriesByDomaine(idDomaine);
            boolean isLastCategory = categoriesDuDomaine.size() == 1;

            // Supprimer la catégorie
            boolean deleted = categorieService.deleteCategorie(id);

            if (deleted) {
                // Supprimer l'image physique si elle existe
                if (imageFileName != null && !imageFileName.trim().isEmpty()) {
                    deleteCategorieImage(imageFileName);
                }

                Map<String, String> response = new HashMap<>();

                // Supprimer le domaine si c'était la dernière catégorie
                if (isLastCategory) {
                    domaineRepository.delete(domaine);
                    // Supprimer aussi l'image du domaine
                    if (domaine.getImage() != null && !domaine.getImage().trim().isEmpty()) {
                        deleteDomaineImage(domaine.getImage());
                    }
                    response.put("message", "Catégorie et domaine supprimés avec succès (c'était la dernière catégorie)");
                    response.put("domaineDeleted", "true");
                } else {
                    response.put("message", "Catégorie supprimée avec succès");
                    response.put("domaineDeleted", "false");
                }

                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = Map.of("message", "Catégorie non trouvée");
                return ResponseEntity.status(404).body(response);
            }

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            Map<String, String> response = Map.of(
                    "message", "Impossible de supprimer cette catégorie car elle contient encore des produits associés."
            );
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = Map.of(
                    "message", "Erreur interne du serveur lors de la suppression de la catégorie."
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // Méthode pour supprimer l'image de catégorie
    private void deleteCategorieImage(String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                System.out.println(" Image catégorie supprimée: " + fileName);
            }
        } catch (Exception e) {
            System.err.println(" Erreur suppression image catégorie: " + e.getMessage());
        }
    }

    // Méthode pour supprimer l'image de domaine
    private void deleteDomaineImage(String fileName) {
        try {
            Path imagePath = Paths.get("assets/domaines/" + fileName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                System.out.println(" Image domaine supprimée: " + fileName);
            }
        } catch (Exception e) {
            System.err.println(" Erreur suppression image domaine: " + e.getMessage());
        }
    }

    // Nouvel endpoint pour compter les catégories d'un domaine
    @GetMapping("/domaine/{idDomaine}/count")
    public ResponseEntity<Long> getCategoriesCountByDomaine(@PathVariable Long idDomaine) {
        try {
            long count = categorieService.getCategoriesByDomaine(idDomaine).size();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}