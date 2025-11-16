package com.marketplace.user.controller;
import jakarta.annotation.PostConstruct;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.CategorieRepository;
import com.marketplace.user.entity.Expert;
import com.marketplace.user.service.ExpertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/experts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;
    private final CategorieRepository categorieRepository;

    // Dossier de stockage des signatures
    private final String SIGNATURES_UPLOAD_DIR = "backend/assets/expert-signatures/";

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(SIGNATURES_UPLOAD_DIR));
            log.info("📁 Dossier des signatures créé ou déjà existant : {}", SIGNATURES_UPLOAD_DIR);
        } catch (IOException e) {
            log.error("❌ Erreur lors de la création du dossier des signatures: {}", e.getMessage());
        }
    }

    // 🔥 MODIFICATION : Méthode pour construire l'URL complète de la photo de profil
    private String buildProfileImageUrl(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        return "http://localhost:8080/api/clients/images/" + fileName;
    }

    // 🔥 MODIFICATION : Méthode pour transformer un Expert en DTO avec URLs complètes
    private Map<String, Object> expertToDto(Expert expert) {
        Map<String, Object> dto = new HashMap<>();

        // Informations de base de l'expert
        dto.put("id", expert.getId());
        dto.put("biography", expert.getBiography());
        dto.put("anneesExperience", expert.getAnneesExperience());
        dto.put("dateEmbauche", expert.getDateEmbauche());
        dto.put("nombreProduitsExpertise", expert.getNombreProduitsExpertise());
        dto.put("active", expert.isActive());
        dto.put("langues", expert.getLangues());
        dto.put("signatureImages", expert.getSignatureImages());

        // Domaine
        if (expert.getDomaine() != null) {
            Map<String, Object> domaineDto = new HashMap<>();
            domaineDto.put("idDomaine", expert.getDomaine().getIdDomaine());
            domaineDto.put("nomDomaine", expert.getDomaine().getNomDomaine());
            domaineDto.put("description", expert.getDomaine().getDescription());
            domaineDto.put("image", expert.getDomaine().getImage());
            dto.put("domaine", domaineDto);
        }

        // Catégories
        if (expert.getCategories() != null) {
            List<Map<String, Object>> categoriesDto = expert.getCategories().stream()
                    .map(categorie -> {
                        Map<String, Object> catDto = new HashMap<>();
                        catDto.put("idCategorie", categorie.getIdCategorie());
                        catDto.put("nomCategorie", categorie.getNomCategorie());
                        catDto.put("description", categorie.getDescription());
                        catDto.put("image", categorie.getImage());
                        return catDto;
                    })
                    .toList();
            dto.put("categories", categoriesDto);
        }

        // Client avec URL complète de la photo de profil
        if (expert.getClient() != null) {
            Map<String, Object> clientDto = new HashMap<>();
            clientDto.put("idclient", expert.getClient().getIdclient());
            clientDto.put("nom", expert.getClient().getNom());
            clientDto.put("prenom", expert.getClient().getPrenom());
            clientDto.put("email", expert.getClient().getEmail());
            clientDto.put("tel", expert.getClient().getTel());
            clientDto.put("pays", expert.getClient().getPays());
            clientDto.put("ville", expert.getClient().getVille());
            clientDto.put("dateinscription", expert.getClient().getDateinscription());
            clientDto.put("enabled", expert.getClient().isEnabled());

            // 🆕 URL COMPLÈTE de la photo de profil
            String profileImageUrl = buildProfileImageUrl(expert.getClient().getPhotoprofil());
            clientDto.put("photoprofil", profileImageUrl);
            clientDto.put("fullName", expert.getClient().getPrenom() + " " + expert.getClient().getNom());

            dto.put("client", clientDto);
        }

        return dto;
    }

    // ==================== ENDPOINTS POUR LES SIGNATURES ====================

    // 🆕 ENDPOINT POUR SERVIR LES IMAGES DE SIGNATURE
    @GetMapping("/expert-signatures/{expertId}/{fileName}")
    public ResponseEntity<Resource> getSignatureImage(
            @PathVariable Long expertId,
            @PathVariable String fileName) {

        try {
            // Construire le chemin complet du fichier
            Path imagePath = Paths.get(SIGNATURES_UPLOAD_DIR + expertId + "/" + fileName);
            Resource resource = new UrlResource(imagePath.toUri());

            log.info("🔍 Tentative de chargement de la signature: {}", imagePath.toString());

            if (resource.exists() && resource.isReadable()) {
                // Déterminer le type de contenu
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                log.info("✅ Signature trouvée: {} (Type: {})", fileName, contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(resource);
            } else {
                log.error("❌ Signature image non trouvée ou illisible: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la lecture de l'image de signature: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // 🆕 UPLOAD AMÉLIORÉ : Stocke une seule fois et remplace les anciennes
    @PostMapping("/{expertId}/upload-signatures")
    public ResponseEntity<?> uploadSignatureImages(
            @PathVariable Long expertId,
            @RequestParam("signatures") List<MultipartFile> files) {

        try {
            log.info("📤 Upload de {} signatures pour l'expert ID: {}", files.size(), expertId);

            // Vérifier que l'expert existe
            if (!expertService.expertExistsById(expertId)) {
                return ResponseEntity.status(404).body("Expert non trouvé");
            }

            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body("Aucun fichier fourni");
            }

            // Validation des fichiers
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest().body("Un ou plusieurs fichiers sont vides");
                }

                if (!file.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body("Seules les images sont autorisées. Fichier rejeté: " + file.getOriginalFilename());
                }

                if (file.getSize() > 2 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body("Fichier trop volumineux: " + file.getOriginalFilename() + " (max 2MB)");
                }
            }

            // Créer le dossier expert s'il n'existe pas
            Path expertDir = Paths.get(SIGNATURES_UPLOAD_DIR + expertId);
            Files.createDirectories(expertDir);
            log.info("📁 Dossier expert créé: {}", expertDir.toString());

            // 🆕 CORRECTION : SUPPRIMER LES ANCIENNES SIGNATURES
            log.info("🗑️ Nettoyage des anciennes signatures...");
            try (var directoryStream = Files.list(expertDir)) {
                directoryStream.forEach(file -> {
                    try {
                        Files.delete(file);
                        log.info("🗑️ Ancienne signature supprimée: {}", file.getFileName());
                    } catch (IOException e) {
                        log.warn("⚠️ Impossible de supprimer l'ancienne signature: {}", file.getFileName());
                    }
                });
            } catch (IOException e) {
                log.warn("⚠️ Aucune ancienne signature à supprimer ou erreur de suppression");
            }

            List<String> uploadedFileNames = new ArrayList<>();

            // 🆕 CORRECTION : Utiliser un compteur séparé pour chaque fichier
            int fileCounter = 1;

            // Sauvegarder chaque fichier
            for (MultipartFile file : files) {
                String originalFileName = file.getOriginalFilename();
                String fileExtension = ".jpg";

                if (originalFileName != null && originalFileName.contains(".")) {
                    fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
                }

                // 🆕 CORRECTION : Générer un nom unique pour CHAQUE fichier
                String newFileName = "signature_" + fileCounter + fileExtension;
                Path filePath = expertDir.resolve(newFileName);

                // Vérifier si le fichier existe déjà (sécurité)
                int safetyCounter = 0;
                while (Files.exists(filePath) && safetyCounter < 10) {
                    fileCounter++;
                    newFileName = "signature_" + fileCounter + fileExtension;
                    filePath = expertDir.resolve(newFileName);
                    safetyCounter++;
                }

                // Sauvegarder le fichier
                Files.copy(file.getInputStream(), filePath);

                uploadedFileNames.add(newFileName);
                log.info("✅ Signature {} sauvegardée: {}", fileCounter, filePath.toString());
                log.info("🔗 URL d'accès: http://localhost:8080/api/experts/expert-signatures/{}/{}", expertId, newFileName);

                // 🆕 INCREMENTER le compteur pour le prochain fichier
                fileCounter++;
            }

            // Mettre à jour l'expert avec les noms des fichiers
            Expert expert = expertService.getExpertById(expertId).orElseThrow();

            // 🆕 CORRECTION : REMPLACER complètement les anciennes signatures
            expert.setSignatureImages(uploadedFileNames);
            expertService.saveExpert(expert);

            log.info("✅ {} signatures enregistrées pour l'expert ID: {}", uploadedFileNames.size(), expertId);

            // Construire les URLs complètes
            List<String> signatureUrls = uploadedFileNames.stream()
                    .map(fileName -> "http://localhost:8080/api/experts/expert-signatures/" + expertId + "/" + fileName)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Signatures uploadées avec succès");
            response.put("uploadedFiles", uploadedFileNames);
            response.put("signatureUrls", signatureUrls);
            response.put("count", uploadedFileNames.size());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("❌ Erreur lors de l'upload des signatures: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur lors de l'upload des signatures: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'upload des signatures: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }

    // ==================== ENDPOINTS EXISTANTS (inchangés) ====================

    @GetMapping("/inactifs/verifies")
    public ResponseEntity<List<Map<String, Object>>> getInactiveExpertsWithVerifiedEmail() {
        try {
            List<Expert> experts = expertService.getInactiveExpertsWithVerifiedEmail();
            List<Map<String, Object>> expertsDto = experts.stream()
                    .map(this::expertToDto)
                    .toList();
            log.info("✅ {} experts inactifs avec email vérifié récupérés", expertsDto.size());
            return ResponseEntity.ok(expertsDto);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des experts inactifs avec email vérifié", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/inactifs")
    public ResponseEntity<List<Map<String, Object>>> getInactiveExperts() {
        try {
            List<Expert> experts = expertService.getInactiveExperts();
            List<Map<String, Object>> expertsDto = experts.stream()
                    .map(this::expertToDto)
                    .toList();
            log.info("✅ {} experts inactifs récupérés avec URLs complètes", expertsDto.size());
            return ResponseEntity.ok(expertsDto);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des experts inactifs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllExperts() {
        try {
            List<Expert> experts = expertService.getAllExperts();
            List<Map<String, Object>> expertsDto = experts.stream()
                    .map(this::expertToDto)
                    .toList();
            return ResponseEntity.ok(expertsDto);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des experts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getExpertById(@PathVariable Long id) {
        try {
            Optional<Expert> expertOpt = expertService.getExpertById(id);
            if (expertOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> expertDto = expertToDto(expertOpt.get());
            return ResponseEntity.ok(expertDto);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'expert ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<Map<String, Object>> getExpertByClientId(@PathVariable Long clientId) {
        try {
            Optional<Expert> expertOpt = expertService.getExpertByClientId(clientId);
            if (expertOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> expertDto = expertToDto(expertOpt.get());
            return ResponseEntity.ok(expertDto);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'expert pour le client ID: {}", clientId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/domaine/{domaineId}")
    public ResponseEntity<List<Map<String, Object>>> getExpertsByDomaine(@PathVariable Long domaineId) {
        try {
            List<Expert> experts = expertService.getExpertsByDomaine(domaineId);
            List<Map<String, Object>> expertsDto = experts.stream()
                    .map(this::expertToDto)
                    .toList();
            return ResponseEntity.ok(expertsDto);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des experts pour le domaine ID: {}", domaineId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createExpert(@RequestBody Expert expert) {
        log.info("📥 REQUETE RECUE - Création d'un expert");
        try {
            // VALIDATION RENFORCÉE
            if (expert.getDomaine() == null || expert.getDomaine().getIdDomaine() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le domaine est obligatoire"));
            }
            if (expert.getClient() == null || expert.getClient().getId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le client est obligatoire"));
            }

            Expert savedExpert = expertService.createExpert(expert);
            log.info("✅ Expert créé avec succès, ID: {}", savedExpert.getId());

            // CRÉATION DU DOSSIER POUR L'EXPERT
            try {
                Path expertDir = Paths.get(SIGNATURES_UPLOAD_DIR + savedExpert.getId());
                Files.createDirectories(expertDir);
                log.info("📁 Dossier créé pour les signatures: {}", expertDir.toString());
            } catch (IOException e) {
                log.warn("⚠️ Impossible de créer le dossier des signatures, mais l'expert a été créé");
            }

            Map<String, Object> expertDto = expertToDto(savedExpert);
            return ResponseEntity.ok(expertDto);

        } catch (IllegalArgumentException e) {
            log.error("❌ Erreur de validation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'expert", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur interne lors de la création de l'expert: " + e.getMessage()));
        }
    }

    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> expertExists(@PathVariable Long id) {
        try {
            boolean exists = expertService.expertExistsById(id);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'existence de l'expert ID: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpert(@PathVariable Long id, @RequestBody Expert expert) {
        log.info("Mise à jour de l'expert ID: {}", id);
        try {
            if (!expertService.expertExistsById(id)) {
                return ResponseEntity.notFound().build();
            }
            expert.setId(id);
            Expert updatedExpert = expertService.saveExpert(expert);
            Map<String, Object> expertDto = expertToDto(updatedExpert);
            return ResponseEntity.ok(expertDto);
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'expert ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la mise à jour de l'expert"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpert(@PathVariable Long id) {
        log.info("Suppression de l'expert ID: {}", id);
        try {
            expertService.deleteExpert(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Expert non trouvé ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'expert ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la suppression de l'expert"));
        }
    }

    @GetMapping("/categories/{domaineId}")
    public ResponseEntity<List<Categorie>> getCategoriesByDomaine(@PathVariable Long domaineId) {
        try {
            List<Categorie> categories = categorieRepository.findByDomaine_IdDomaine(domaineId);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des catégories pour le domaine ID: {}", domaineId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/langues")
    public ResponseEntity<List<String>> getLanguesDisponibles() {
        try {
            List<String> langues = Arrays.asList("Français", "Anglais", "Arabe", "Espagnol", "Allemand");
            return ResponseEntity.ok(langues);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des langues", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/check-expert/{clientId}")
    public ResponseEntity<Map<String, Object>> checkExpertStatus(@PathVariable Long clientId) {
        log.info("🔍 Vérification statut expert pour le client ID: {}", clientId);

        try {
            if (clientId == null || clientId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("isExpert", false);
                errorResponse.put("status", "INVALID_ID");
                errorResponse.put("message", "ID client invalide");
                errorResponse.put("clientId", clientId);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Map<String, Object> status = expertService.checkExpertStatusByClientId(clientId);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du statut expert: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isExpert", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("clientId", clientId);
            errorResponse.put("error", "Erreur serveur");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateExpert(@PathVariable Long id) {

        boolean activated = expertService.activateExpert(id);

        if (!activated) {
            return ResponseEntity.status(404)
                    .body("Expert non trouvé ou erreur d'activation");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Expert activé avec succès");
        response.put("expertId", id);
        response.put("active", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/actifs")
    public ResponseEntity<?> getExpertsActifs() {
        List<Expert> experts = expertService.findByIsActiveTrue();
        return ResponseEntity.ok(experts);
    }


}