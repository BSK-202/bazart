package com.marketplace.catalog.controller;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.service.CategorieService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategorieController {

    private final CategorieService categorieService;

    // 📁 Dossier de stockage des images de catégories
    private final String UPLOAD_DIR = "backend/assets/categories/";

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
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
}