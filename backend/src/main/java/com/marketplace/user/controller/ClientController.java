package com.marketplace.user.controller;

import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;
    private final String UPLOAD_DIR = "backend/assets/user/";

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du dossier: " + e.getMessage());
        }
    }

    // 🆕 NOUVEAU: Endpoint pour servir les images de profil
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
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
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            } else {
                System.err.println("❌ Profile image not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Error reading profile image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

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
            Client client = clientOpt.get();
            System.out.println("✅ Client trouvé - ID en BDD: " + client.getIdclient() + ", Nom: " + client.getNom() + " " + client.getPrenom());

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
            if (!fileExtension.equals(".jpg") && !fileExtension.equals(".jpeg") &&
                    !fileExtension.equals(".png") && !fileExtension.equals(".gif")) {
                fileExtension = ".jpg"; // Forcer jpg si extension non supportée
            }

            Long actualClientId = client.getIdclient();
            String newFileName = actualClientId + fileExtension; // ✅ Utilise actualClientId de la BDD
            Path filePath = Paths.get(UPLOAD_DIR + newFileName);
            System.out.println("🆔 ID client depuis URL: " + clientId + ", ID client depuis BDD: " + actualClientId);
            System.out.println("📁 Nom de fichier généré: " + newFileName);
            String[] possibleExtensions = {".jpg", ".jpeg", ".png", ".gif"};
            // Supprimer l'ancienne image si elle existe
            //Files.deleteIfExists(filePath);
            for (String ext : possibleExtensions) {
                Path oldFilePath = Paths.get(UPLOAD_DIR + actualClientId + ext);
                if (Files.exists(oldFilePath)) {
                    Files.delete(oldFilePath);
                    System.out.println("🗑️ Ancien fichier supprimé: " + oldFilePath);
                }
            }


            // Sauvegarder la nouvelle image
            Files.copy(file.getInputStream(), filePath);

            System.out.println("✅ Photo de profil sauvegardée: " + filePath.toString());

            // Mettre à jour le nom de fichier dans la base de données

            client.setPhotoprofil(newFileName);
            Client updatedClient = clientService.updateClient(client);
            //clientService.updateClient(client);
            System.out.println("✅ BDD mise à jour avec photoprofil: " + updatedClient.getPhotoprofil());

            // Vérifier que la mise à jour a bien fonctionné
            Optional<Client> verifyClient = clientService.getClientById(actualClientId);
            if (verifyClient.isPresent()) {
                System.out.println("🔍 Vérification BDD - photoprofil stocké: " + verifyClient.get().getPhotoprofil());
            }
            // Construire l'URL complète
            String profileImageUrl = "http://localhost:8080/api/clients/images/" + newFileName;

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", newFileName);
            response.put("profileImageUrl", profileImageUrl);
            response.put("photoProfil", profileImageUrl); // ✅ Champ que le frontend attend
            response.put("message", "Photo de profil uploadée avec succès");
            response.put("clientId", actualClientId);

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
    @GetMapping("/{id}")
    public ResponseEntity<?> getClientById(@PathVariable Long id) {
        try {
            System.out.println("🔍 Recherche client avec ID: " + id);

            Optional<Client> clientOpt = clientService.getClientById(id);

            if (clientOpt.isEmpty()) {
                System.out.println("❌ Client non trouvé pour ID: " + id);
                return ResponseEntity.status(404).body("Client non trouvé");
            }

            Client client = clientOpt.get();
            System.out.println("✅ Client trouvé: " + client.getNom() + " " + client.getPrenom());

            // 🆕 MODIFICATION: Construire l'URL complète de l'image
            String profileImageUrl = null;
            if (client.getPhotoprofil() != null && !client.getPhotoprofil().trim().isEmpty()) {
                profileImageUrl = "http://localhost:8080/api/clients/images/" + client.getPhotoprofil();
                System.out.println("🖼 URL image de profil générée: " + profileImageUrl);
            }

            // Créer un DTO pour éviter d'exposer des données sensibles
            Map<String, Object> response = new HashMap<>();
            response.put("idClient", client.getIdclient());
            response.put("nom", client.getNom());
            response.put("prenom", client.getPrenom());
            response.put("email", client.getEmail());
            response.put("tel", client.getTel());
            response.put("ville", client.getVille());
            response.put("pays", client.getPays());
            response.put("dateInscription", client.getDateinscription());
            response.put("photoProfil", profileImageUrl); // 🆕 URL complète
            response.put("photoProfilFileName", client.getPhotoprofil()); // 🆕 Garder aussi le nom de fichier
            response.put("enabled", client.isEnabled());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération du client: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getClientByEmail(@PathVariable String email) {
        try {
            Optional<Client> clientOpt = clientService.findByEmail(email);

            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Client non trouvé");
            }

            Client client = clientOpt.get();

            // 🆕 MODIFICATION: Construire l'URL complète de l'image
            String profileImageUrl = null;
            if (client.getPhotoprofil() != null && !client.getPhotoprofil().trim().isEmpty()) {
                profileImageUrl = "http://localhost:8080/api/clients/images/" + client.getPhotoprofil();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("idClient", client.getIdclient());
            response.put("nom", client.getNom());
            response.put("prenom", client.getPrenom());
            response.put("email", client.getEmail());
            response.put("tel", client.getTel());
            response.put("ville", client.getVille());
            response.put("pays", client.getPays());
            response.put("dateInscription", client.getDateinscription());
            response.put("photoProfil", profileImageUrl); // 🆕 URL complète
            response.put("photoProfilFileName", client.getPhotoprofil()); // 🆕 Garder aussi le nom de fichier

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur serveur");
        }
    }

    // Les autres méthodes restent inchangées...
    @PostMapping("/{clientId}/upload-profile-image")
    public ResponseEntity<?> uploadProfileImage(
            @PathVariable Long clientId,
            @RequestParam("file") MultipartFile file) {

        try {
            System.out.println("📤 Upload d'image pour le client ID: " + clientId);

            Optional<Client> clientOpt = clientService.getClientById(clientId);
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Client non trouvé");
            }
            Client client = clientOpt.get();
            Long actualClientId = client.getIdclient();
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

            String newFileName = actualClientId  + fileExtension;
            Path filePath = Paths.get(UPLOAD_DIR + newFileName);

            String[] possibleExtensions = {".jpg", ".jpeg", ".png", ".gif"};
            for (String ext : possibleExtensions) {
                Path oldFilePath = Paths.get(UPLOAD_DIR + actualClientId + ext);
                Files.deleteIfExists(oldFilePath);
            }
            Files.copy(file.getInputStream(), filePath);

            System.out.println("✅ Image sauvegardée: " + filePath.toString());
            client.setPhotoprofil(newFileName);
            clientService.updateClient(client);
            // 🆕 MODIFICATION: Retourner l'URL complète
            String profileImageUrl = "http://localhost:8080/api/clients/images/" + newFileName;

            Map<String, String> response = new HashMap<>();
            response.put("fileName", newFileName);
            response.put("profileImageUrl", profileImageUrl); // 🆕 URL complète
            response.put("message", "Image uploadée avec succès");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'upload de l'image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de l'upload de l'image: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }

    @PatchMapping("/{clientId}/profile-image")
    public ResponseEntity<?> updateProfileImageName(
            @PathVariable Long clientId,
            @RequestBody Map<String, String> updateData) {

        try {
            System.out.println("🔄 Mise à jour de l'image de profil pour le client ID: " + clientId);

            Optional<Client> clientOpt = clientService.getClientById(clientId);
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Client non trouvé");
            }

            Client client = clientOpt.get();
            String fileName = updateData.get("photoprofil");

            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nom de fichier invalide");
            }

            client.setPhotoprofil(fileName);
            clientService.updateClient(client);

            System.out.println("✅ Nom de l'image de profil mis à jour: " + fileName);

            // 🆕 MODIFICATION: Retourner l'URL complète
            String profileImageUrl = "http://localhost:8080/api/clients/images/" + fileName;

            Map<String, String> response = new HashMap<>();
            response.put("message", "Nom de l'image de profil mis à jour avec succès");
            response.put("fileName", fileName);
            response.put("profileImageUrl", profileImageUrl); // 🆕 URL complète

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<?> updateClient(
            @PathVariable Long clientId,
            @RequestBody Map<String, Object> clientData) {

        try {
            System.out.println("✏️ [BACKEND] Mise à jour du client ID: " + clientId);

            // 🔍 Récupérer le client existant
            Optional<Client> clientOpt = clientService.getClientById(clientId);
            if (clientOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Client non trouvé");
            }

            Client client = clientOpt.get();

            // 🔄 Mettre à jour les champs s'ils sont présents
            if (clientData.containsKey("nom")) client.setNom((String) clientData.get("nom"));
            if (clientData.containsKey("prenom")) client.setPrenom((String) clientData.get("prenom"));
            if (clientData.containsKey("email")) client.setEmail((String) clientData.get("email"));
            if (clientData.containsKey("tel")) client.setTel((String) clientData.get("tel"));
            if (clientData.containsKey("ville")) client.setVille((String) clientData.get("ville"));
            if (clientData.containsKey("pays")) client.setPays((String) clientData.get("pays"));
            if (clientData.containsKey("photoprofil")) client.setPhotoprofil((String) clientData.get("photoprofil"));
            if (clientData.containsKey("enabled")) client.setEnabled((Boolean) clientData.get("enabled"));

            // 💾 Sauvegarder les modifications
            Client updatedClient = clientService.updateClient(client);

            // Construire la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("idClient", updatedClient.getIdclient());
            response.put("nom", updatedClient.getNom());
            response.put("prenom", updatedClient.getPrenom());
            response.put("email", updatedClient.getEmail());
            response.put("tel", updatedClient.getTel());
            response.put("ville", updatedClient.getVille());
            response.put("pays", updatedClient.getPays());
            response.put("dateInscription", updatedClient.getDateinscription());
            response.put("photoProfil", updatedClient.getPhotoprofil() != null
                    ? "http://localhost:8080/api/clients/images/" + updatedClient.getPhotoprofil()
                    : null);
            response.put("enabled", updatedClient.isEnabled());

            System.out.println("✅ Client mis à jour avec succès: " + updatedClient.getNom());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour du client: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur: " + e.getMessage());
        }
    }


    @PutMapping("/verify-email/{email}")
    public ResponseEntity<?> verifyEmail(@PathVariable String email) {
        clientService.updateEmailVerified(email);
        return ResponseEntity.ok("Email verified updated");
    }

}