package com.marketplace.expertise.service.impl;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.expertise.dto.ExpertiseRequestDTO;
import com.marketplace.expertise.entity.*;
import com.marketplace.expertise.repository.ExpertiseRequestRepository;
import com.marketplace.expertise.repository.ExpertiseSlotRepository;
import com.marketplace.expertise.service.ExpertiseService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.entity.Expert;
import com.marketplace.user.repository.ExpertRepository;
import com.marketplace.wallet.service.Walletservice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Transactional
public class ExpertiseServiceImpl implements ExpertiseService {

    private final ProduitRepository produitRepository;
    private final ExpertiseRequestRepository expertiseRequestRepository;
    private final ExpertiseSlotRepository expertiseSlotRepository;
    private final ExpertRepository expertRepository;
    private final Walletservice walletservice;
    private final NotificationService notificationService;

    // ---- PARAMS DEADLINES ----
    private static final double ONLINE_PRICE = 50.0;
    private static final double ONSITE_PRICE = 100.0;
    private static final int EXPERT_ACTION_RESPONSE_HOURS = 1;   // Temps pour accepter/refuser la première demande
    private static final int REPORT_SUBMISSION_DAYS = 1;           // Nombre de jours pour soumettre le rapport après acceptation
    private static final double EXPERT_PAYOUT_RATE = 0.7; // 70% pour l'expert
    // -------------------------


    // ==== 1) CRÉATION DE LA DEMANDE APRÈS ACCEPTATION ADMIN ====
    @Override
    public void createRequestAfterProductAccepted(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        if (!produit.isAExpertise()) { return; }
        if (produit.getVendeur() == null) {
            throw new IllegalStateException("Produit sans vendeur");
        }

        // Si ONSITE, on doit avoir au moins 1 créneau proposé
        if (produit.getExpertiseMethod() == ExpertiseMethod.ONSITE) {
            if (produit.getExpertiseSlot1() == null
                    && produit.getExpertiseSlot2() == null
                    && produit.getExpertiseSlot3() == null) {
                throw new IllegalStateException("Aucun créneau d'expertise proposé pour expertise sur site");
            }
        }

        ExpertiseRequest request = new ExpertiseRequest();
        request.setProduit(produit);
        request.setVendeur(produit.getVendeur());
        request.setMethod(produit.getExpertiseMethod() != null ? produit.getExpertiseMethod() : ExpertiseMethod.ONLINE);
        request.setStatus(ExpertiseStatus.CREATED);
        double price = request.getMethod() == ExpertiseMethod.ONLINE ? ONLINE_PRICE : ONSITE_PRICE;
        request.setPrice(price);
        Client vendeur = produit.getVendeur();

        // Deadline pour accepter/refuser la demande
        // Deadline pour accepter/refuser la demande
        request.setExpertResponseDeadline(LocalDateTime.now().plusMinutes(EXPERT_ACTION_RESPONSE_HOURS));

// Location précisée si sur place
        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            request.setLocation("Magasin Bazart, Avenue XXX, Ville YYY");
        } else {
            request.setLocation("ONLINE");
        }

// DEADLINE RAPPORT : SEULEMENT POUR ONLINE, PAS POUR ONSITE
        if (request.getMethod() == ExpertiseMethod.ONLINE) {
            request.setReportSubmissionDeadline(LocalDateTime.now().plusMinutes(REPORT_SUBMISSION_DAYS));
        } else {
            // ONSITE : pas de deadline fixe, sera disponible après le rendez-vous
            request.setReportSubmissionDeadline(null);
        }
        ExpertiseRequest savedRequest = expertiseRequestRepository.save(request);

        // Enregistrer les slots (si ONSITE)
        List<ExpertiseSlot> slots = new ArrayList<>();
        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            if (produit.getExpertiseSlot1() != null) {
                slots.add(createSlot(1, produit.getExpertiseSlot1(), savedRequest));
            }
            if (produit.getExpertiseSlot2() != null) {
                slots.add(createSlot(2, produit.getExpertiseSlot2(), savedRequest));
            }
            if (produit.getExpertiseSlot3() != null) {
                slots.add(createSlot(3, produit.getExpertiseSlot3(), savedRequest));
            }
            expertiseSlotRepository.saveAll(slots);
            savedRequest.setSlots(slots);
        } else {
            savedRequest.setSlots(Collections.emptyList());
        }

        produit.setExpertiseRequestId(savedRequest.getId());
        produit.setEtat_expertise("en_attente_expertise");
        produitRepository.save(produit);

        assignExpertAndNotify(savedRequest);
    }

    private ExpertiseSlot createSlot(int idx, LocalDateTime dt, ExpertiseRequest req) {
        ExpertiseSlot slot = new ExpertiseSlot();
        slot.setSlotIndex(idx);
        slot.setDateTime(dt);
        slot.setExpertiseRequest(req);
        return slot;
    }

    // Appel par défaut (sans exclusion)
    private void assignExpertAndNotify(ExpertiseRequest request) {
        assignExpertAndNotify(request, request.getExcludedExpertClientIds());
    }

    // Implémentation avec exclusion - AMÉLIORÉE
    private void assignExpertAndNotify(ExpertiseRequest request, Set<Long> excludeIds) {
        Produit produit = request.getProduit();
        if (produit.getCategorie() == null || produit.getCategorie().getDomaine() == null) return;

        Long domaineId = produit.getCategorie().getDomaine().getIdDomaine();

        // Experts actifs du domaine
        List<Expert> expertsDomain = expertRepository.findByDomaineId(domaineId).stream()
                .filter(Expert::isActive)
                .toList();

        if (expertsDomain.isEmpty()) {
            handleNoExpertsAvailable(request, produit);
            return;
        }

        // Filtre catégorie
        Long categorieId = produit.getCategorie().getIdCategorie();
        List<Expert> expertsCategory = expertsDomain.stream()
                .filter(expert -> expert.getCategories() != null &&
                        expert.getCategories().stream().anyMatch(cat -> cat.getIdCategorie().equals(categorieId)))
                .toList();

        List<Expert> candidates = expertsCategory.isEmpty() ? expertsDomain : expertsCategory;

        // Exclure les experts déjà essayés
        if (excludeIds != null && !excludeIds.isEmpty()) {
            candidates = candidates.stream()
                    .filter(ex -> ex.getClient() != null && !excludeIds.contains(ex.getClient().getIdclient()))
                    .toList();
        }

        if (candidates.isEmpty()) {
            // Si tous les experts ont été essayés
            if (excludeIds != null && excludeIds.size() >= expertsDomain.size()) {
                handleAllExpertsTried(request, produit);
            } else {
                handleNoExpertsAvailable(request, produit);
            }
            return;
        }

        // Choisir l'expert le moins chargé
        Expert chosen = candidates.stream()
                .min(Comparator.comparingInt(Expert::getNombreProduitsExpertise))
                .orElseThrow();

        // Assignation
        request.setExpert(chosen);
        request.setStatus(ExpertiseStatus.PENDING_EXPERT_DECISION);
        request.setExpertResponseDeadline(LocalDateTime.now().plusMinutes(EXPERT_ACTION_RESPONSE_HOURS));
        expertiseRequestRepository.save(request);

        // Notification à l'expert
        if (chosen.getClient() != null) {
            Long expertUserId = chosen.getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();
            expertNotif.put("productName", produit.getNom());
            expertNotif.put("expertiseMethod", request.getMethod().name());
            expertNotif.put("productId", produit.getIdproduit());
            expertNotif.put("requestId", request.getId());
            expertNotif.put("deadline", request.getExpertResponseDeadline().toString());
            expertNotif.put("message",
                    "Vous avez une nouvelle demande d'expertise sur le produit \"" + produit.getNom() + "\". " +
                            (request.getMethod() == ExpertiseMethod.ONSITE
                                    ? "Veuillez consulter les créneaux proposés et accepter/refuser avant " +
                                    request.getExpertResponseDeadline().toLocalTime() + "."
                                    : "Veuillez accepter/refuser pour expertise en ligne avant " +
                                    request.getExpertResponseDeadline().toLocalTime() + ".")
            );
            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);
        }

        // Notification au vendeur
        Map<String, Object> vendeurNotif = new HashMap<>();
        vendeurNotif.put("message", "Un expert a été assigné à votre produit \"" + produit.getNom() +
                "\". Attente de sa réponse (délai: " + EXPERT_ACTION_RESPONSE_HOURS + " heure(s)).");
        notificationService.processEvent(NotificationType.GENERIC,
                Set.of(request.getVendeur().getIdclient()), vendeurNotif);
    }

    private void handleNoExpertsAvailable(ExpertiseRequest request, Produit produit) {
        request.setStatus(ExpertiseStatus.NO_EXPERTS_AVAILABLE);
        request.setExpert(null); // Réinitialiser l'expert assigné
        expertiseRequestRepository.save(request);

        // Mettre à jour l'état du produit avec un état plus explicite
        produit.setEtat_expertise("attente_expert_disponible");
        produitRepository.save(produit);

        // Notification DÉTAILLÉE au vendeur
        Map<String, Object> vendeurData = new HashMap<>();
        vendeurData.put("productName", produit.getNom());
        vendeurData.put("productId", produit.getIdproduit());
        vendeurData.put("status", "NO_EXPERTS_AVAILABLE");
        vendeurData.put("message",
                "⚠️ Aucun expert disponible actuellement\n\n" +
                        "Produit : \"" + produit.getNom() + "\"\n" +
                        "Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Non spécifiée") + "\n\n" +
                        "📋 Détails :\n" +
                        "• Méthode d'expertise : " + request.getMethod() + "\n" +
                        "• Statut : En attente d'expert disponible\n" +
                        "• Créneau : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n" +
                        "⚡ Système automatique :\n" +
                        "Le système va réessayer automatiquement d'assigner un expert toutes les heures.\n\n" +
                        "📞 Support :\n" +
                        "Si l'attente persiste plus de 24h, contactez notre support pour obtenir de l'aide."
        );

        notificationService.processEvent(
                NotificationType.MESSAGE,
                Set.of(request.getVendeur().getIdclient()),
                vendeurData
        );

        // Notification aux administrateurs
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("productName", produit.getNom());
        adminData.put("productId", produit.getIdproduit());
        adminData.put("vendeurName", request.getVendeur().getPrenom() + " " + request.getVendeur().getNom());
        adminData.put("category", produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "N/A");
        adminData.put("domain", produit.getCategorie() != null && produit.getCategorie().getDomaine() != null
                ? produit.getCategorie().getDomaine().getNomDomaine() : "N/A");
        adminData.put("message",
                "🚨 Alerte : Aucun expert disponible\n\n" +
                        "Produit : " + produit.getNom() + " (ID: " + produit.getIdproduit() + ")\n" +
                        "Vendeur : " + request.getVendeur().getPrenom() + " " + request.getVendeur().getNom() + "\n" +
                        "Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "N/A") + "\n" +
                        "Domaine : " + (produit.getCategorie() != null && produit.getCategorie().getDomaine() != null
                        ? produit.getCategorie().getDomaine().getNomDomaine() : "N/A") + "\n\n" +
                        "Action requise :\n" +
                        "1. Vérifier la disponibilité des experts dans ce domaine\n" +
                        "2. Contacter un expert manuellement si nécessaire\n" +
                        "3. Informer le vendeur des délais supplémentaires"
        );

        // Vous devriez avoir une méthode pour récupérer les IDs des administrateurs
        // notificationService.processEvent(NotificationType.ADMIN_ALERT, adminUserIds, adminData);
    }

    // Gestion quand tous les experts ont été essayés
    private void handleAllExpertsTried(ExpertiseRequest request, Produit produit) {
        request.setStatus(ExpertiseStatus.ALL_EXPERTS_TRIED);
        request.setExpert(null);
        request.setExpertResponseDeadline(null); // Supprimer toute deadline
        expertiseRequestRepository.save(request);

        produit.setEtat_expertise("tous_experts_contactes");
        produitRepository.save(produit);

        // Notification UNIQUE et CLAIRE au vendeur
        Map<String, Object> vendeurData = new HashMap<>();
        vendeurData.put("productName", produit.getNom());
        vendeurData.put("productId", produit.getIdproduit());
        vendeurData.put("status", "ALL_EXPERTS_TRIED");
        vendeurData.put("message",
                "🚨 Situation exceptionnelle - Processus d'expertise en pause\n\n" +
                        "Produit : \"" + produit.getNom() + "\"\n" +
                        "Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Non spécifiée") + "\n\n" +
                        "📊 État actuel :\n" +
                        "Tous les experts disponibles (" + request.getExcludedExpertClientIds().size() + ") ont été contactés mais aucun n'a pu accepter.\n\n" +
                        "⏰ Historique :\n" +
                        "• Première tentative : " + request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n" +
                        "• Dernière tentative : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n" +
                        "⏸️ Système en pause :\n" +
                        "Le système a temporairement arrêté les tentatives automatiques pour éviter les notifications répétitives.\n\n" +
                        "🔄 Prochaine tentative automatique :\n" +
                        "Dans 24 heures (le " + LocalDateTime.now().plusHours(24).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")\n\n" +
                        "📞 Action immédiate (optionnelle) :\n" +
                        "Si vous souhaitez accélérer le processus, contactez notre support client.\n" +
                        "Email : support@bazart.com\n" +
                        "Téléphone : +XXX XXX XXX"
        );

        notificationService.processEvent(
                NotificationType.MESSAGE,
                Set.of(request.getVendeur().getIdclient()),
                vendeurData
        );
    }
    // ==== 2) MÉTHODE POUR RÉESSAYER LES DEMANDES BLOQUÉES ====
    @Override
    public void retryBlockedRequests() {
        List<ExpertiseStatus> blockedStatuses = Arrays.asList(
                ExpertiseStatus.NO_EXPERTS_AVAILABLE,
                ExpertiseStatus.ALL_EXPERTS_TRIED
        );

        List<ExpertiseRequest> blockedRequests = expertiseRequestRepository
                .findByStatusIn(blockedStatuses);

        for (ExpertiseRequest req : blockedRequests) {
            // CAS 1: ALL_EXPERTS_TRIED - Réinitialisation après 24h seulement
            if (req.getStatus() == ExpertiseStatus.ALL_EXPERTS_TRIED) {
                // Réinitialiser les exclusions après 24h
                LocalDateTime createdAt = req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.now();

                // Vérifier si 24h se sont écoulées
                if (LocalDateTime.now().isAfter(createdAt.plusHours(24))) {
                    req.getExcludedExpertClientIds().clear();
                    req.setStatus(ExpertiseStatus.CREATED);
                    expertiseRequestRepository.save(req);

                    // Notifier le vendeur UNE SEULE FOIS de la réinitialisation
                    Map<String, Object> resetNotif = new HashMap<>();
                    resetNotif.put("message",
                            "🔄 Réinitialisation du processus d'expertise\n\n" +
                                    "Produit : \"" + req.getProduit().getNom() + "\"\n\n" +
                                    "Après 24 heures d'attente, le système a réinitialisé la recherche d'expert.\n" +
                                    "Tous les experts sont à nouveau disponibles pour évaluer votre produit.\n\n" +
                                    "📅 Nouvelle tentative en cours..."
                    );
                    notificationService.processEvent(
                            NotificationType.MESSAGE,
                            Set.of(req.getVendeur().getIdclient()),
                            resetNotif
                    );

                    // Réassigner un expert
                    assignExpertAndNotify(req, req.getExcludedExpertClientIds());
                }
                // Si moins de 24h, NE RIEN FAIRE - éviter les notifications répétitives
                continue;
            }

            // CAS 2: NO_EXPERTS_AVAILABLE - Réessayer immédiatement
            if (req.getStatus() == ExpertiseStatus.NO_EXPERTS_AVAILABLE) {
                req.setStatus(ExpertiseStatus.CREATED);
                expertiseRequestRepository.save(req);

                // Notifier le vendeur du réessai (UNIQUEMENT pour NO_EXPERTS_AVAILABLE)
                Map<String, Object> vendeurNotif = new HashMap<>();
                vendeurNotif.put("message",
                        "🔄 Nouvelle tentative d'assignation d'expert\n\n" +
                                "Produit : \"" + req.getProduit().getNom() + "\"\n\n" +
                                "Le système relance automatiquement la recherche d'un expert disponible.\n" +
                                "Vous recevrez une notification dès qu'un expert sera assigné.\n\n" +
                                "⏳ Statut : En cours de recherche..."
                );
                notificationService.processEvent(
                        NotificationType.MESSAGE,
                        Set.of(req.getVendeur().getIdclient()),
                        vendeurNotif
                );

                assignExpertAndNotify(req, req.getExcludedExpertClientIds());
            }
        }
    }
    // ==== 3) LISTE DES DEMANDES POUR UN EXPERT ====
    @Override
    @Transactional(readOnly = true)
    public List<ExpertiseRequestDTO> getPendingRequestsForExpert(Long clientId) {
        Expert expert = expertRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Expert non trouvé"));
        List<ExpertiseRequest> requests = expertiseRequestRepository
                .findByExpertAndStatus(expert, ExpertiseStatus.PENDING_EXPERT_DECISION);
        List<ExpertiseRequestDTO> result = new ArrayList<>();
        for (ExpertiseRequest req : requests) {
            result.add(toDto(req));
        }
        return result;
    }

    // ==== 4) ACCEPTATION (ET CHOIX SLOT) PAR L'EXPERT ====
    @Override
    public ExpertiseRequestDTO acceptRequestAndChooseSlot(Long requestId, Long expertId, Integer slotIndex) {
        ExpertiseRequest request = expertiseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'expertise non trouvée"));

        // Vérifier sur l'ID client de l'expert
        Long assignedClientId = request.getExpert() != null && request.getExpert().getClient() != null
                ? request.getExpert().getClient().getIdclient()
                : null;
        if (assignedClientId == null || !assignedClientId.equals(expertId)) {
            throw new IllegalStateException("Cette demande n'est pas assignée à cet expert");
        }

        if (request.getStatus() != ExpertiseStatus.PENDING_EXPERT_DECISION) {
            throw new IllegalStateException("La demande n'est pas en attente de décision expert");
        }
        if (request.getExpertResponseDeadline() != null && LocalDateTime.now().isAfter(request.getExpertResponseDeadline())) {
            throw new IllegalStateException("Le délai de réponse est dépassé !");
        }

        // Deadline pour soumettre le rapport
        // Deadline pour soumettre le rapport
        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            // Choix du slot
            if (slotIndex == null)
                throw new IllegalArgumentException("Un créneau doit être choisi pour une expertise présentielle (ONSITE)");

            Optional<ExpertiseSlot> chosenOpt = request.getSlots().stream()
                    .filter(s -> s.getSlotIndex() == slotIndex)
                    .findFirst();
            if (chosenOpt.isEmpty())
                throw new IllegalArgumentException("Créneau invalide");
            ExpertiseSlot chosen = chosenOpt.get();
            chosen.setChosen(true);
            expertiseSlotRepository.save(chosen);

            request.setConfirmedDateTime(chosen.getDateTime());
            // ONSITE : pas de deadline fixe pour le rapport, formulaire disponible après le rendez-vous
            request.setReportSubmissionDeadline(null);
        } else {
            // Online : deadline rapport à partir de l'acceptation
            request.setConfirmedDateTime(null);
            request.setReportSubmissionDeadline(LocalDateTime.now().plusMinutes(REPORT_SUBMISSION_DAYS));
        }

        request.setStatus(ExpertiseStatus.PLANNED);
        request.setExpertResponseDeadline(null); // Deadline d'action terminée

        // Débit du wallet du vendeur
        Client vendeur = request.getVendeur();
        double price = request.getPrice() != null ? request.getPrice() :
                (request.getMethod() == ExpertiseMethod.ONLINE ? ONLINE_PRICE : ONSITE_PRICE);
        request.setPrice(price);

        /*String description = "Frais d'expertise " +
                (request.getMethod() == ExpertiseMethod.ONLINE ? "en ligne" : "présentielle") +
                " pour le produit \"" + request.getProduit().getNom() + "\" (ID " + request.getProduit().getIdproduit() + ")";
        walletservice.debitWallet(vendeur, price, description);*/

        expertiseRequestRepository.save(request);

        // Notif vendeur
        Map<String, Object> vendeurNotif = new HashMap<>();
        vendeurNotif.put("productName", request.getProduit().getNom());
        vendeurNotif.put("dateTime", request.getConfirmedDateTime() != null ? request.getConfirmedDateTime().toString() : null);
        vendeurNotif.put("expertiseMethod", request.getMethod() == ExpertiseMethod.ONLINE ? "en ligne" : "présentielle");
        vendeurNotif.put("amount", price);

        notificationService.processEvent(NotificationType.PRODUCT_EXPERTISE_PLANNED,
                Set.of(vendeur.getIdclient()), vendeurNotif);

        // Notif expert confirmée
        if (request.getExpert().getClient() != null) {
            Long expertUserId = request.getExpert().getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();
            expertNotif.put("productName", request.getProduit().getNom());
            expertNotif.put("dateTime", request.getConfirmedDateTime() != null ? request.getConfirmedDateTime().toString() : null);
// Ne mettre la deadline que si elle existe (uniquement pour ONLINE)
            if (request.getReportSubmissionDeadline() != null) {
                expertNotif.put("deadline", request.getReportSubmissionDeadline().toString());
            } else {
                // Pour ONSITE, indiquer qu'il n'y a pas de deadline fixe
                expertNotif.put("deadline", "Aucune deadline fixe - Formulaire disponible après le rendez-vous");
            }            expertNotif.put("message", "Vous avez accepté l'expertise pour le produit \"" +
                    request.getProduit().getNom() + "\"" +
                    (request.getConfirmedDateTime() != null ? " le " + request.getConfirmedDateTime() : "") );
            notificationService.processEvent(NotificationType.GENERIC, Set.of(expertUserId), expertNotif);
        }

        // Mettre à jour état du produit
        Produit produit = request.getProduit();
        produit.setEtat_expertise("expertise_planifiee");
        produitRepository.save(produit);

        return toDto(request);
    }

    // ==== 5) REFUS PAR L'EXPERT ====
    @Override
    public ExpertiseRequestDTO refuseExpertiseRequest(Long requestId, Long expertId) {
        ExpertiseRequest request = expertiseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'expertise non trouvée"));

        Long assignedClientId = request.getExpert() != null && request.getExpert().getClient() != null
                ? request.getExpert().getClient().getIdclient()
                : null;
        if (assignedClientId == null || !assignedClientId.equals(expertId)) {
            throw new IllegalStateException("Demande non assignée à cet expert !");
        }

        // Exclure cet expert pour les prochaines assignations
        if (assignedClientId != null) {
            request.getExcludedExpertClientIds().add(assignedClientId);
        }

        // Notification à l'expert
        if (request.getExpert() != null && request.getExpert().getClient() != null) {
            Map<String, Object> expertNotif = new HashMap<>();
            expertNotif.put("message", "Vous avez refusé l'expertise pour le produit \"" +
                    request.getProduit().getNom() + "\".");
            notificationService.processEvent(NotificationType.GENERIC,
                    Set.of(request.getExpert().getClient().getIdclient()), expertNotif);
        }

        // Réinitialiser la demande
        request.setExpert(null);
        request.setStatus(ExpertiseStatus.CREATED);
        request.setExpertResponseDeadline(null);
        request.setReportSubmissionDeadline(null);
        expertiseRequestRepository.save(request);

        // Notif vendeur (refus)
        Map<String, Object> vendeurNotif = new HashMap<>();
        vendeurNotif.put("message", "L'expert a refusé la demande d'expertise pour le produit \"" +
                request.getProduit().getNom() + "\". Un nouvel expert sera assigné.");
        notificationService.processEvent(NotificationType.GENERIC,
                Set.of(request.getVendeur().getIdclient()), vendeurNotif);

        // Réassignation en excluant tous les experts déjà essayés
        assignExpertAndNotify(request, request.getExcludedExpertClientIds());

        return toDto(request);
    }

    // ==== 6) SCHEDULER POUR TRAITER LES DEADLINES - AMÉLIORÉ ====
    @Override
    public void processExpiredExpertDecisions() {
        LocalDateTime now = LocalDateTime.now();
        List<ExpertiseRequest> expired = expertiseRequestRepository
                .findByStatusAndExpertResponseDeadlineBefore(ExpertiseStatus.PENDING_EXPERT_DECISION, now);

        for (ExpertiseRequest req : expired) {
            // Ajouter l'expert actuel à la liste d'exclusion
            if (req.getExpert() != null && req.getExpert().getClient() != null) {
                Long expertClientId = req.getExpert().getClient().getIdclient();
                req.getExcludedExpertClientIds().add(expertClientId);

                // Notifier l'expert qui a dépassé le délai
                Map<String, Object> expertNotif = new HashMap<>();
                expertNotif.put("message", "Vous avez dépassé le délai de réponse pour l'expertise du produit \"" +
                        req.getProduit().getNom() + "\". La demande a été réassignée à un autre expert.");
                notificationService.processEvent(
                        NotificationType.GENERIC,
                        Set.of(expertClientId),
                        expertNotif
                );
            }

            // Réinitialiser la demande
            req.setExpert(null);
            req.setStatus(ExpertiseStatus.CREATED);
            req.setExpertResponseDeadline(null);
            expertiseRequestRepository.save(req);

            // Notifier le vendeur
            Map<String, Object> vendeurNotif = new HashMap<>();
            vendeurNotif.put("message", "L'expert n'a pas répondu dans les délais pour le produit \"" +
                    req.getProduit().getNom() + "\". Réassignation à un nouvel expert en cours.");
            notificationService.processEvent(
                    NotificationType.GENERIC,
                    Set.of(req.getVendeur().getIdclient()),
                    vendeurNotif
            );

            // Réassigner à un nouvel expert (en excluant ceux déjà essayés)
            assignExpertAndNotify(req, req.getExcludedExpertClientIds());
        }
    }

    @Override
    public void processExpiredReportSubmissions() {
        LocalDateTime now = LocalDateTime.now();

        // Récupérer toutes les demandes avec deadline expirée
        List<ExpertiseRequest> expired = expertiseRequestRepository
                .findByStatusAndReportSubmissionDeadlineBefore(ExpertiseStatus.PLANNED, now);

        // Filtrer pour ne garder que les ONLINE (ONSITE n'a pas de deadline)
        List<ExpertiseRequest> onlineExpired = expired.stream()
                .filter(req -> req.getMethod() == ExpertiseMethod.ONLINE)
                .collect(Collectors.toList());

        for (ExpertiseRequest req : onlineExpired) {
            // Ajouter l'expert actuel à la liste d'exclusion
            if (req.getExpert() != null && req.getExpert().getClient() != null) {
                Long expertClientId = req.getExpert().getClient().getIdclient();
                req.getExcludedExpertClientIds().add(expertClientId);

                // Notifier l'expert qui n'a pas soumis le rapport
                Map<String, Object> expertNotif = new HashMap<>();
                expertNotif.put("message", "Vous avez dépassé le délai de soumission du rapport pour l'expertise du produit \"" +
                        req.getProduit().getNom() + "\". La demande a été réassignée.");
                notificationService.processEvent(
                        NotificationType.GENERIC,
                        Set.of(expertClientId),
                        expertNotif
                );
            }

            // Notifier le vendeur
            Map<String, Object> vendeurNotif = new HashMap<>();
            vendeurNotif.put("message", "L'expert n'a pas soumis le rapport dans les délais pour le produit \"" +
                    req.getProduit().getNom() + "\". Réassignation à un autre expert en cours.");
            notificationService.processEvent(
                    NotificationType.GENERIC,
                    Set.of(req.getVendeur().getIdclient()),
                    vendeurNotif
            );

            // Réinitialiser la demande
            req.setStatus(ExpertiseStatus.CREATED);
            req.setExpert(null);
            req.setReportSubmissionDeadline(null);
            req.setConfirmedDateTime(null);

            // Réinitialiser les slots si ONSITE
            if (req.getMethod() == ExpertiseMethod.ONSITE) {
                for (ExpertiseSlot slot : req.getSlots()) {
                    slot.setChosen(false);
                }
                expertiseSlotRepository.saveAll(req.getSlots());
            }

            expertiseRequestRepository.save(req);

            // Réassigner à un nouvel expert
            assignExpertAndNotify(req, req.getExcludedExpertClientIds());
        }
    }

    // ==== 7) CONVERSION DTO ====
    private ExpertiseRequestDTO toDto(ExpertiseRequest req) {
        ExpertiseRequestDTO dto = new ExpertiseRequestDTO();
        dto.setId(req.getId());
        dto.setProduitId(req.getProduit().getIdproduit());
        dto.setProduitNom(req.getProduit().getNom());
        dto.setProduitDescription(req.getProduit().getDescription());
        dto.setProduitEtat(req.getProduit().getEtat_expertise());
        dto.setProduitCategorie(
                req.getProduit().getCategorie() != null
                        ? req.getProduit().getCategorie().getNomCategorie()
                        : null
        );
        dto.setPrixDebut(req.getProduit().getPrixDebut());
        dto.setPrixFin(req.getProduit().getPrixFin());

        // Acheteur peut être null
        dto.setAcheteurNom(
                req.getProduit().getAcheteur() != null
                        ? req.getProduit().getAcheteur().getNom()
                        : null
        );
        dto.setDatePublication(
                req.getProduit().getDatePublication() != null
                        ? req.getProduit().getDatePublication().toString()
                        : null
        );

        // Images : première + liste complète
        if (req.getProduit().getImages() != null && !req.getProduit().getImages().isEmpty()) {
            var images = req.getProduit().getImages()
                    .stream()
                    .map(img -> img.getUrl())
                    .toList();
            dto.setProduitImages(images);
            dto.setProduitImage(images.get(0));
        }

        dto.setVendeurId(req.getVendeur().getIdclient());
        dto.setVendeurNom(req.getVendeur().getPrenom() + " " + req.getVendeur().getNom());
        dto.setExpertId(req.getExpert() != null && req.getExpert().getClient() != null
                ? req.getExpert().getClient().getIdclient()
                : null);
        dto.setExpertFullName(req.getExpert() != null && req.getExpert().getClient() != null
                ? req.getExpert().getClient().getPrenom() + " " + req.getExpert().getClient().getNom()
                : null);
        dto.setMethod(req.getMethod());
        dto.setStatus(req.getStatus());

        double totalPrice = req.getPrice() != null
                ? req.getPrice()
                : (req.getMethod() == ExpertiseMethod.ONLINE ? ONLINE_PRICE : ONSITE_PRICE);
        double expertShare = totalPrice * EXPERT_PAYOUT_RATE;

        dto.setPrice(totalPrice);      // montant total payé par le vendeur
        dto.setExpertShare(expertShare); // part de l'expert

        dto.setConfirmedDateTime(req.getConfirmedDateTime() != null ? req.getConfirmedDateTime().toString() : null);
        dto.setLocation(req.getLocation());
        if (req.getExpertResponseDeadline() != null)
            dto.setExpertResponseDeadline(req.getExpertResponseDeadline().toString());
        if (req.getReportSubmissionDeadline() != null)
            dto.setReportSubmissionDeadline(req.getReportSubmissionDeadline().toString());

        List<String> slotStrings = new ArrayList<>();
        for (ExpertiseSlot slot : req.getSlots()) {
            slotStrings.add(slot.getDateTime().toString());
        }
        dto.setSlots(slotStrings);

        return dto;
    }

    @Override
    public ExpertiseRequestDTO getRequestDetailById(Long requestId) {
        ExpertiseRequest req = expertiseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'expertise non trouvée"));
        return toDto(req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpertiseRequestDTO> getAssignedRequestsForExpert(Long clientId) {
        Expert expert = expertRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Expert non trouvé"));
        var statuses = List.of(ExpertiseStatus.PENDING_EXPERT_DECISION, ExpertiseStatus.PLANNED);
        return expertiseRequestRepository.findByExpertAndStatusIn(expert, statuses)
                .stream().map(this::toDto).toList();
    }

    @Override
    public ExpertiseRequestDTO reopenAfterSellerInfo(Long requestId) {
        ExpertiseRequest req = expertiseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'expertise non trouvée"));

        // reset assignment and deadlines
        req.setExpert(null);
        req.setStatus(ExpertiseStatus.CREATED);
        req.setExpertResponseDeadline(LocalDateTime.now().plusMinutes(EXPERT_ACTION_RESPONSE_HOURS));
        req.setReportSubmissionDeadline(null);
        expertiseRequestRepository.save(req);

        // product back to waiting-for-expertise
        Produit produit = req.getProduit();
        produit.setEtat_expertise("en_attente_expertise");
        produit.setExpertiseApproved(false);
        produitRepository.save(produit);

        // reassign an expert
        assignExpertAndNotify(req);

        // notify seller
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Votre produit \"" + produit.getNom() + "\" a été remis en file d'expertise après ajout d'informations.");
        notificationService.processEvent(NotificationType.GENERIC, Set.of(req.getVendeur().getIdclient()), data);

        return toDto(req);
    }

    @Override
    public boolean canSubmitReport(Long requestId) {
        ExpertiseRequest request = expertiseRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));

        if (request.getMethod() == ExpertiseMethod.ONLINE) {
            // ONLINE : accessible immédiatement après acceptation
            return request.getStatus() == ExpertiseStatus.PLANNED;
        } else {
            // ONSITE : accessible uniquement après la date du rendez-vous
            if (request.getConfirmedDateTime() == null) {
                return false;
            }
            return LocalDateTime.now().isAfter(request.getConfirmedDateTime());
        }
    }
}