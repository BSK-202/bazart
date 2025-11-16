package com.marketplace.user.service;

import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.repository.DomaineRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.entity.Expert;
import com.marketplace.user.repository.ClientRepository;
import com.marketplace.user.repository.ExpertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertServiceImpl implements ExpertService {

    private final ExpertRepository expertRepository;
    private final DomaineRepository domaineRepository;
    private final ClientRepository clientRepository;

    @Override
    public List<Expert> getAllExperts() {
        log.info("Récupération de tous les experts");
        return expertRepository.findAll();
    }

    @Override
    public Optional<Expert> getExpertById(Long id) {
        log.info("Récupération de l'expert avec ID: {}", id);
        return expertRepository.findById(id);
    }

    @Override
    public Expert saveExpert(Expert expert) {
        log.info("Sauvegarde de l'expert: {}", expert);
        return expertRepository.save(expert);
    }

    @Override
    @Transactional
    public Expert createExpert(Expert expert) {
        log.info("🚀 Début création expert: {}", expert);

        if (expert == null) {
            throw new IllegalArgumentException("Les informations de l'expert ne peuvent pas être nulles.");
        }

        // Validation du domaine
        if (expert.getDomaine() == null) {
            throw new IllegalArgumentException("L'objet domaine est null");
        }

        if (expert.getDomaine().getIdDomaine() == null) {
            throw new IllegalArgumentException("L'ID du domaine est null");
        }

        Long domaineId = expert.getDomaine().getIdDomaine();
        log.info("🔍 Recherche du domaine avec ID: {}", domaineId);

        Domaine domaine = domaineRepository.findById(domaineId)
                .orElseThrow(() -> {
                    String error = "Domaine non trouvé avec l'ID: " + domaineId;
                    log.error("❌ {}", error);
                    return new IllegalArgumentException(error);
                });

        log.info("✅ Domaine trouvé: {} (ID: {})", domaine.getNomDomaine(), domaine.getIdDomaine());
        expert.setDomaine(domaine);

        // Validation du client
        if (expert.getClient() == null) {
            throw new IllegalArgumentException("L'objet client est null");
        }

        if (expert.getClient().getId() == null) {
            throw new IllegalArgumentException("L'ID du client est null");
        }

        Long clientId = expert.getClient().getId();
        log.info("🔍 Recherche du client avec ID: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> {
                    String error = "Client non trouvé avec l'ID: " + clientId;
                    log.error("❌ {}", error);
                    return new IllegalArgumentException(error);
                });

        log.info("✅ Client trouvé: {} {} (ID: {})", client.getPrenom(), client.getNom(), client.getId());
        expert.setClient(client);

        // Date d'embauche nullable
        if (expert.getDateEmbauche() == null) {
            expert.setDateEmbauche(null);
            log.info("📅 Date d'embauche définie sur null");
        }

        // Valeurs par défaut
        expert.setActive(false);

        // Traitement des noms de signatures
        if (expert.getSignatureImages() != null && !expert.getSignatureImages().isEmpty()) {
            log.info("📋 {} noms de signatures à traiter", expert.getSignatureImages().size());
            // Les noms seront traités et les fichiers uploadés séparément
        }

        Expert savedExpert = expertRepository.save(expert);
        log.info("✅ Expert créé avec succès, ID: {}", savedExpert.getId());

        return savedExpert;
    }

    @Override
    @Transactional
    public Expert createExpertWithClient(Long clientId, Expert expert) {
        log.info("Création d'un expert pour le client ID: {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client non trouvé avec l'ID: " + clientId));

        expert.setClient(client);

        return createExpert(expert);
    }

    @Override
    public boolean expertExistsById(Long id) {
        return expertRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteExpert(Long id) {
        log.info("Suppression de l'expert avec ID: {}", id);
        if (expertRepository.existsById(id)) {
            expertRepository.deleteById(id);
            log.info("Expert supprimé avec succès, ID: {}", id);
        } else {
            throw new IllegalArgumentException("Expert non trouvé avec l'ID: " + id);
        }
    }

    @Override
    public Optional<Expert> getExpertByClientId(Long clientId) {
        log.info("Récupération de l'expert pour le client ID: {}", clientId);
        return expertRepository.findByClientId(clientId);
    }

    @Override
    public List<Expert> getExpertsByDomaine(Long domaineId) {
        log.info("Récupération des experts pour le domaine ID: {}", domaineId);
        return expertRepository.findByDomaineId(domaineId);
    }

    @Override
    public List<Expert> getInactiveExperts() {
        log.info("Récupération des experts inactifs");
        return expertRepository.findByIsActiveFalse();
    }




    // 🔥 SIMPLIFICATION : L'upload est maintenant géré dans le Controller
    @Override
    public List<String> uploadSignatureFiles(Long expertId, List<MultipartFile> signatureFiles) throws IOException {
        // Cette méthode n'est plus utilisée car l'upload est géré dans le Controller
        throw new UnsupportedOperationException("L'upload des signatures est maintenant géré dans le Controller");
    }

    @Override
    public List<Expert> getInactiveExpertsWithVerifiedEmail() {
        log.info("Récupération des experts inactifs avec email vérifié");
        return expertRepository.findByIsActiveFalseAndClientEmailVerifiedTrue();
    }

    @Override
    public Map<String, Object> checkExpertStatusByClientId(Long clientId) {
        log.info("🔍 Vérification du statut expert pour le client ID: {}", clientId);

        Map<String, Object> response = new HashMap<>();

        try {
            // Vérifier d'abord si le client existe
            boolean clientExists = clientRepository.existsById(clientId);
            if (!clientExists) {
                response.put("isExpert", false);
                response.put("status", "NOT_CLIENT");
                response.put("message", "Client non trouvé");
                response.put("clientId", clientId);
                log.warn("❌ Client non trouvé avec ID: {}", clientId);
                return response;
            }

            // Vérifier si le client est expert
            Optional<Boolean> activeStatus = expertRepository.findActiveStatusByClientId(clientId);

            if (activeStatus.isPresent()) {
                Boolean isActive = activeStatus.get();
                response.put("isExpert", true);
                response.put("isActive", isActive);
                response.put("status", isActive ? "ACTIVE" : "INACTIVE");
                response.put("clientId", clientId);
                response.put("message", isActive ? "Expert actif" : "Expert inactif");

                log.info("✅ Client ID {} est expert - Statut: {}", clientId, isActive ? "ACTIF" : "INACTIF");
            } else {
                response.put("isExpert", false);
                response.put("isActive", false);
                response.put("status", "NOT_EXPERT");
                response.put("clientId", clientId);
                response.put("message", "Ce client n'est pas expert");

                log.info("ℹ️ Client ID {} n'est pas expert", clientId);
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification du statut expert: {}", e.getMessage());
            response.put("isExpert", false);
            response.put("isActive", false);
            response.put("status", "ERROR");
            response.put("clientId", clientId);
            response.put("error", "Erreur lors de la vérification");
        }

        return response;
    }

    @Override
    @Transactional
    public boolean activateExpert(Long id) {
        log.info("Activation de l'expert ID: {}", id);

        Optional<Expert> expertOpt = expertRepository.findById(id);
        if (expertOpt.isEmpty()) {
            log.warn("❌ Expert non trouvé avec ID: {}", id);
            return false;
        }

        // Méthode UPDATE via repository
        int updated = expertRepository.activateExpert(id);

        if (updated > 0) {
            log.info("✅ Expert ID {} activé avec succès", id);
            return true;
        }

        log.error("❌ Erreur lors de l'activation de l'expert ID {}", id);
        return false;
    }

    @Override
    public List<Expert> findByIsActiveTrue() {
        return expertRepository.findByIsActiveTrue();
    }


}