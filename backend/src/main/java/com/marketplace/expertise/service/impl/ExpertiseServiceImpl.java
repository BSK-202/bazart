package com.marketplace.expertise.service.impl;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.expertise.dto.ExpertiseRequestDTO;
import com.marketplace.expertise.entity.*;
import com.marketplace.expertise.repository.ExpertiseReportRepository;
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
    private final ExpertiseReportRepository expertiseReportRepository; // AJOUTER CETTE INJECTION

    // ---- PARAMS DEADLINES ----
    private static final double ONLINE_PRICE = 50.0;
    private static final double ONSITE_PRICE = 100.0;
    private static final int EXPERT_ACTION_RESPONSE_HOURS = 1;   // Temps pour accepter/refuser la première demande
    private static final int REPORT_SUBMISSION_DAYS = 1;           // Nombre de jours pour soumettre le rapport après acceptation
    private static final double EXPERT_PAYOUT_RATE = 0.7; // 70% pour l'expert

    // ---- TYPES DE NOTIFICATION ----
    private static final String NOTIF_TYPE_EXPERT_ASSIGNED = "EXPERT_ASSIGNED";
    private static final String NOTIF_TYPE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    private static final String NOTIF_TYPE_REQUEST_REFUSED = "REQUEST_REFUSED";
    private static final String NOTIF_TYPE_EXPIRED_DEADLINE = "EXPIRED_DEADLINE";
    private static final String NOTIF_TYPE_REPORT_DEADLINE_EXPIRED = "REPORT_DEADLINE_EXPIRED";
    private static final String NOTIF_TYPE_NO_EXPERTS = "NO_EXPERTS_AVAILABLE";
    private static final String NOTIF_TYPE_ALL_EXPERTS_TRIED = "ALL_EXPERTS_TRIED";

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

    // Méthodes utilitaires pour formater les dates
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Non spécifié";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        return dateTime.format(formatter);
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "Non calculé";

        long minutes = java.time.Duration.between(start, end).toMinutes();

        if (minutes < 60) {
            return minutes + " minute(s)";
        } else if (minutes < 1440) {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            return hours + " heure(s) " + (remainingMinutes > 0 ? remainingMinutes + " minute(s)" : "");
        } else {
            long days = minutes / 1440;
            long remainingHours = (minutes % 1440) / 60;
            return days + " jour(s) " + (remainingHours > 0 ? remainingHours + " heure(s)" : "");
        }
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

        // NOTIFICATION AMÉLIORÉE À L'EXPERT
        if (chosen.getClient() != null) {
            Long expertUserId = chosen.getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();

            String urgency = request.getExpertResponseDeadline().isBefore(LocalDateTime.now().plusHours(6))
                    ? "URGENT" : "NORMAL";

            StringBuilder message = new StringBuilder();
            message.append("📋 Nouvelle demande d'expertise\n\n");
            message.append("Produit : ").append(produit.getNom()).append("\n");
            message.append("Référence : PROD-").append(produit.getIdproduit()).append("\n");
            message.append("Vendeur : ").append(request.getVendeur().getPrenom()).append(" ")
                    .append(request.getVendeur().getNom()).append("\n\n");

            if (produit.getCategorie() != null) {
                message.append("Catégorie : ").append(produit.getCategorie().getNomCategorie()).append("\n");
            }

            message.append("Méthode : ").append(request.getMethod() == ExpertiseMethod.ONSITE
                    ? "Expertise sur site" : "Expertise en ligne").append("\n");

            if (request.getMethod() == ExpertiseMethod.ONSITE) {
                message.append("📍 Adresse : ").append(request.getLocation() != null
                        ? request.getLocation() : "À définir").append("\n");
                message.append("📅 Créneaux proposés :\n");
                for (int i = 0; i < request.getSlots().size(); i++) {
                    ExpertiseSlot slot = request.getSlots().get(i);
                    message.append(i+1).append(". ").append(formatDateTime(slot.getDateTime())).append("\n");
                }
            }

            message.append("\n⏰ Délai de réponse : ");
            message.append(formatDateTime(request.getExpertResponseDeadline())).append("\n");
            message.append("Statut : ").append(urgency).append("\n\n");

            message.append("📞 Contact vendeur si besoin :\n");
            message.append("- Email : ").append(request.getVendeur().getEmail()).append("\n");

            message.append("\n➡️ Actions possibles :\n");
            message.append("• Accepter et choisir un créneau (ONSITE)\n");
            message.append("• Accepter directement (ONLINE)\n");
            message.append("• Refuser (un nouvel expert sera contacté)\n");

            expertNotif.put("message", message.toString());
            expertNotif.put("productName", produit.getNom());
            expertNotif.put("productId", produit.getIdproduit());
            expertNotif.put("requestId", request.getId());
            expertNotif.put("deadline", request.getExpertResponseDeadline().toString());
            expertNotif.put("urgency", urgency);
            expertNotif.put("method", request.getMethod().name());
            expertNotif.put("notificationType", NOTIF_TYPE_EXPERT_ASSIGNED);

            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);
        }

        // NOTIFICATION AMÉLIORÉE AU VENDEUR
        Map<String, Object> vendeurNotif = new HashMap<>();

        StringBuilder vendeurMessage = new StringBuilder();
        vendeurMessage.append("✅ Expert assigné\n\n");
        vendeurMessage.append("Produit : ").append(produit.getNom()).append("\n");
        vendeurMessage.append("Expert : ").append(chosen.getClient().getPrenom()).append(" ")
                .append(chosen.getClient().getNom()).append("\n");
        vendeurMessage.append("Spécialité : ").append(chosen.getDomaine() != null
                ? chosen.getDomaine().getNomDomaine() : "Général").append("\n\n");

        vendeurMessage.append("📊 Informations expert :\n");
        vendeurMessage.append("• Nombre d'expertises réalisées : ").append(chosen.getNombreProduitsExpertise()).append("\n");


        vendeurMessage.append("⏳ Prochaines étapes :\n");
        vendeurMessage.append("L'expert a ").append(EXPERT_ACTION_RESPONSE_HOURS)
                .append(" heure(s) pour accepter ou refuser la demande.\n");
        vendeurMessage.append("Vous serez notifié dès qu'une décision sera prise.\n\n");

        vendeurMessage.append("📞 Support :\n");
        vendeurMessage.append("Pour toute question, contactez-nous à support@bazart.com");

        vendeurNotif.put("message", vendeurMessage.toString());
        vendeurNotif.put("productName", produit.getNom());
        vendeurNotif.put("expertName", chosen.getClient().getPrenom() + " " + chosen.getClient().getNom());
        vendeurNotif.put("expertiseMethod", request.getMethod().name());
        vendeurNotif.put("notificationType", NOTIF_TYPE_EXPERT_ASSIGNED);

        notificationService.processEvent(NotificationType.MESSAGE,
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

        StringBuilder message = new StringBuilder();
        message.append("⚠️ Aucun expert disponible actuellement\n\n");
        message.append("Produit : \"").append(produit.getNom()).append("\"\n");
        message.append("Catégorie : ").append(produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Non spécifiée").append("\n\n");
        message.append("📋 Détails :\n");
        message.append("• Méthode d'expertise : ").append(request.getMethod()).append("\n");
        message.append("• Statut : En attente d'expert disponible\n");
        message.append("• Créneau : ").append(formatDateTime(LocalDateTime.now())).append("\n\n");
        message.append("⚡ Système automatique :\n");
        message.append("Le système va réessayer automatiquement d'assigner un expert toutes les heures.\n\n");
        message.append("📞 Support :\n");
        message.append("Si l'attente persiste plus de 24h, contactez notre support pour obtenir de l'aide.\n");
        message.append("Email : support@bazart.com\n");
        message.append("Téléphone : +XXX XXX XXX");

        vendeurData.put("message", message.toString());
        vendeurData.put("notificationType", NOTIF_TYPE_NO_EXPERTS);

        notificationService.processEvent(
                NotificationType.MESSAGE,
                Set.of(request.getVendeur().getIdclient()),
                vendeurData
        );
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

        StringBuilder message = new StringBuilder();
        message.append("🚨 Situation exceptionnelle - Processus d'expertise en pause\n\n");
        message.append("Produit : \"").append(produit.getNom()).append("\"\n");
        message.append("Catégorie : ").append(produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Non spécifiée").append("\n\n");
        message.append("📊 État actuel :\n");
        message.append("Tous les experts disponibles (").append(request.getExcludedExpertClientIds().size()).append(") ont été contactés mais aucun n'a pu accepter.\n\n");
        message.append("⏰ Historique :\n");
        message.append("• Première tentative : ").append(formatDateTime(request.getCreatedAt())).append("\n");
        message.append("• Dernière tentative : ").append(formatDateTime(LocalDateTime.now())).append("\n\n");
        message.append("⏸️ Système en pause :\n");
        message.append("Le système a temporairement arrêté les tentatives automatiques pour éviter les notifications répétitives.\n\n");
        message.append("🔄 Prochaine tentative automatique :\n");
        message.append("Dans 24 heures (le ").append(formatDateTime(LocalDateTime.now().plusHours(24))).append(")\n\n");
        message.append("📞 Action immédiate (optionnelle) :\n");
        message.append("Si vous souhaitez accélérer le processus, contactez notre support client.\n");
        message.append("Email : support@bazart.com\n");
        message.append("Téléphone : +XXX XXX XXX");

        vendeurData.put("message", message.toString());
        vendeurData.put("notificationType", NOTIF_TYPE_ALL_EXPERTS_TRIED);

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

                    StringBuilder message = new StringBuilder();
                    message.append("🔄 Réinitialisation du processus d'expertise\n\n");
                    message.append("Produit : \"").append(req.getProduit().getNom()).append("\"\n\n");
                    message.append("Après 24 heures d'attente, le système a réinitialisé la recherche d'expert.\n");
                    message.append("Tous les experts sont à nouveau disponibles pour évaluer votre produit.\n\n");
                    message.append("📅 Nouvelle tentative en cours...");

                    resetNotif.put("message", message.toString());
                    resetNotif.put("notificationType", NOTIF_TYPE_ALL_EXPERTS_TRIED);

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

                StringBuilder message = new StringBuilder();
                message.append("🔄 Nouvelle tentative d'assignation d'expert\n\n");
                message.append("Produit : \"").append(req.getProduit().getNom()).append("\"\n\n");
                message.append("Le système relance automatiquement la recherche d'un expert disponible.\n");
                message.append("Vous recevrez une notification dès qu'un expert sera assigné.\n\n");
                message.append("⏳ Statut : En cours de recherche...");

                vendeurNotif.put("message", message.toString());
                vendeurNotif.put("notificationType", NOTIF_TYPE_NO_EXPERTS);

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

        // NOTIFICATION AMÉLIORÉE AU VENDEUR
        Map<String, Object> vendeurNotif = new HashMap<>();

        StringBuilder vendeurMessage = new StringBuilder();
        vendeurMessage.append("✅ Expertise acceptée\n\n");
        vendeurMessage.append("Produit : ").append(request.getProduit().getNom()).append("\n");
        vendeurMessage.append("Expert : ").append(request.getExpert().getClient().getPrenom())
                .append(" ").append(request.getExpert().getClient().getNom()).append("\n\n");

        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            vendeurMessage.append("📅 Rendez-vous confirmé :\n");
            vendeurMessage.append("Date : ").append(formatDateTime(request.getConfirmedDateTime())).append("\n");
            vendeurMessage.append("Lieu : ").append(request.getLocation() != null
                    ? request.getLocation() : "Magasin Bazart").append("\n\n");

            vendeurMessage.append("📝 Préparation rendez-vous :\n");
            vendeurMessage.append("• Présentez-vous 15 minutes à l'avance\n");
            vendeurMessage.append("• Apportez le produit et les accessoires\n");
            vendeurMessage.append("• Pensez à la facture d'achat (si disponible)\n\n");
        } else {
            vendeurMessage.append("📋 Expertise en ligne\n");
            vendeurMessage.append("L'expert va maintenant analyser les photos de votre produit.\n");
            vendeurMessage.append("Délai de traitement : ").append(REPORT_SUBMISSION_DAYS)
                    .append(" jour(s) maximum\n\n");
        }

        vendeurMessage.append("💰 Montant débité : ").append(String.format("%.2f DH", price)).append("\n");
        vendeurMessage.append("Cette somme inclut la commission de la plateforme.\n\n");

        vendeurMessage.append("⏳ Prochaine étape :\n");
        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            vendeurMessage.append("1. Rendez-vous d'expertise\n");
            vendeurMessage.append("2. Rapport d'expertise (après le rendez-vous)\n");
        } else {
            vendeurMessage.append("1. Analyse par l'expert\n");
            vendeurMessage.append("2. Rapport d'expertise (dans les ").append(REPORT_SUBMISSION_DAYS)
                    .append(" jours)\n");
        }
        vendeurMessage.append("3. Décision finale\n\n");

        vendeurMessage.append("📞 Support :\n");
        vendeurMessage.append("Pour toute question, contactez-nous à support@bazart.com");

        vendeurNotif.put("message", vendeurMessage.toString());
        vendeurNotif.put("productName", request.getProduit().getNom());
        vendeurNotif.put("dateTime", request.getConfirmedDateTime() != null
                ? formatDateTime(request.getConfirmedDateTime()) : null);
        vendeurNotif.put("amount", price);
        vendeurNotif.put("notificationType", NOTIF_TYPE_REQUEST_ACCEPTED);

        notificationService.processEvent(NotificationType.PRODUCT_EXPERTISE_PLANNED,
                Set.of(vendeur.getIdclient()), vendeurNotif);

        // NOTIFICATION AMÉLIORÉE À L'EXPERT
        if (request.getExpert().getClient() != null) {
            Long expertUserId = request.getExpert().getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();

            StringBuilder expertMessage = new StringBuilder();
            expertMessage.append("✅ Demande acceptée\n\n");
            expertMessage.append("Produit : ").append(request.getProduit().getNom()).append("\n");
            expertMessage.append("Vendeur : ").append(vendeur.getPrenom()).append(" ")
                    .append(vendeur.getNom()).append("\n");
            expertMessage.append("Téléphone : ").append(vendeur.getPhotoprofil() != null
                    ? vendeur.getPhotoprofil() : "Non communiqué").append("\n");
            expertMessage.append("Email : ").append(vendeur.getEmail()).append("\n\n");

            if (request.getMethod() == ExpertiseMethod.ONSITE) {
                expertMessage.append("📅 Rendez-vous confirmé :\n");
                expertMessage.append("Date : ").append(formatDateTime(request.getConfirmedDateTime())).append("\n");
                expertMessage.append("Lieu : ").append(request.getLocation() != null
                        ? request.getLocation() : "Magasin Bazart").append("\n\n");

                expertMessage.append("📋 Actions après rendez-vous :\n");
                expertMessage.append("1. Effectuer l'expertise\n");
                expertMessage.append("2. Soumettre le rapport via l'interface\n");
                expertMessage.append("3. Gagner : ").append(String.format("%.2f DH", price * EXPERT_PAYOUT_RATE)).append("\n\n");
            } else {
                expertMessage.append("📋 Expertise en ligne\n");
                expertMessage.append("Analysez les photos du produit et rédigez le rapport.\n\n");

                if (request.getReportSubmissionDeadline() != null) {
                    expertMessage.append("⏰ Délai de soumission : ")
                            .append(formatDateTime(request.getReportSubmissionDeadline())).append("\n");
                }
                expertMessage.append("💰 Rémunération : ")
                        .append(String.format("%.2f DH", price * EXPERT_PAYOUT_RATE)).append("\n\n");
            }

            expertMessage.append("📞 Contact vendeur :\n");
            expertMessage.append("- Email : ").append(vendeur.getEmail()).append("\n");
            if (vendeur.getPhotoprofil() != null) {
                expertMessage.append("- Téléphone : ").append(vendeur.getPhotoprofil()).append("\n");
            }

            expertMessage.append("\n➡️ Accès au produit :\n");
            expertMessage.append("Consultez toutes les photos et détails dans votre interface expert.");

            expertNotif.put("message", expertMessage.toString());
            expertNotif.put("productName", request.getProduit().getNom());
            expertNotif.put("dateTime", request.getConfirmedDateTime() != null
                    ? formatDateTime(request.getConfirmedDateTime()) : null);
            expertNotif.put("deadline", request.getReportSubmissionDeadline() != null
                    ? formatDateTime(request.getReportSubmissionDeadline()) : "Après rendez-vous");
            expertNotif.put("notificationType", NOTIF_TYPE_REQUEST_ACCEPTED);

            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);
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

        // NOTIFICATION AMÉLIORÉE À L'EXPERT
        if (request.getExpert() != null && request.getExpert().getClient() != null) {
            Map<String, Object> expertNotif = new HashMap<>();

            String expertMessage = "❌ Demande refusée\n\n" +
                    "Produit : " + request.getProduit().getNom() + "\n" +
                    "Vendeur : " + request.getVendeur().getPrenom() + " " + request.getVendeur().getNom() + "\n" +
                    "Méthode : " + (request.getMethod() == ExpertiseMethod.ONSITE ? "Sur site" : "En ligne") + "\n\n" +
                    "✅ Action enregistrée :\n" +
                    "• Vous avez été retiré de cette demande\n" +
                    "• Un nouvel expert va être contacté\n\n" +
                    "📊 Votre disponibilité :\n" +
                    "Pour éviter d'être contacté pour des demandes similaires, " +
                    "mettez à jour votre calendrier dans votre profil expert.";

            expertNotif.put("message", expertMessage);
            expertNotif.put("notificationType", NOTIF_TYPE_REQUEST_REFUSED);
            expertNotif.put("productName", request.getProduit().getNom());

            notificationService.processEvent(NotificationType.MESSAGE,
                    Set.of(request.getExpert().getClient().getIdclient()), expertNotif);
        }

        // Réinitialiser la demande
        request.setExpert(null);
        request.setStatus(ExpertiseStatus.CREATED);
        request.setExpertResponseDeadline(null);
        request.setReportSubmissionDeadline(null);
        expertiseRequestRepository.save(request);

        // NOTIFICATION AMÉLIORÉE AU VENDEUR
        Map<String, Object> vendeurNotif = new HashMap<>();

        String vendeurMessage = "🔄 Nouvel expert en cours d'assignation\n\n" +
                "Produit : " + request.getProduit().getNom() + "\n" +
                "Expert précédent : " + (request.getExpert() != null && request.getExpert().getClient() != null
                ? request.getExpert().getClient().getPrenom() + " " + request.getExpert().getClient().getNom()
                : "Non spécifié") + "\n\n" +
                "📋 Statut :\n" +
                "L'expert assigné n'a pas pu prendre en charge votre demande.\n\n" +
                "⚡ Système automatique :\n" +
                "Un nouvel expert est en cours de contact.\n" +
                "Délai de réponse : " + EXPERT_ACTION_RESPONSE_HOURS + " heure(s)\n\n" +
                "📊 Statistiques :\n" +
                "• Experts contactés : " + (request.getExcludedExpertClientIds().size() + 1) + "\n" +
                "• Délai moyen : 15-30 minutes\n\n" +
                "⏳ Suivi en temps réel :\n" +
                "Vous serez notifié dès qu'un expert acceptera.";

        vendeurNotif.put("message", vendeurMessage);
        vendeurNotif.put("notificationType", NOTIF_TYPE_REQUEST_REFUSED);
        vendeurNotif.put("productName", request.getProduit().getNom());

        notificationService.processEvent(NotificationType.MESSAGE,
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

                // NOTIFICATION AMÉLIORÉE À L'EXPERT
                Map<String, Object> expertNotif = new HashMap<>();

                String expertMessage = "⏰ Délai de réponse dépassé\n\n" +
                        "Produit : " + req.getProduit().getNom() + "\n" +
                        "Vendeur : " + req.getVendeur().getPrenom() + " " + req.getVendeur().getNom() + "\n" +
                        "Délai initial : " + formatDateTime(req.getExpertResponseDeadline()) + "\n\n" +
                        "⚠️ Conséquences :\n" +
                        "• Vous avez été retiré de cette demande\n" +
                        "• La demande a été réassignée à un autre expert\n" +
                        "• Cela peut affecter votre taux de réponse\n\n" +
                        "💡 Pour éviter cela à l'avenir :\n" +
                        "1. Vérifiez vos notifications régulièrement\n" +
                        "2. Mettez à jour votre disponibilité\n" +
                        "3. Configurez les rappels si nécessaire";

                expertNotif.put("message", expertMessage);
                expertNotif.put("notificationType", NOTIF_TYPE_EXPIRED_DEADLINE);
                expertNotif.put("productName", req.getProduit().getNom());

                notificationService.processEvent(
                        NotificationType.MESSAGE,
                        Set.of(expertClientId),
                        expertNotif
                );
            }

            // Réinitialiser la demande
            req.setExpert(null);
            req.setStatus(ExpertiseStatus.CREATED);
            req.setExpertResponseDeadline(null);
            expertiseRequestRepository.save(req);

            // NOTIFICATION AMÉLIORÉE AU VENDEUR
            Map<String, Object> vendeurNotif = new HashMap<>();

            String vendeurMessage = "🔄 Réassignation automatique\n\n" +
                    "Produit : " + req.getProduit().getNom() + "\n" +
                    "Expert précédent : " + (req.getExpert() != null && req.getExpert().getClient() != null
                    ? req.getExpert().getClient().getPrenom() + " " + req.getExpert().getClient().getNom()
                    : "Non spécifié") + "\n\n" +
                    "📋 Statut :\n" +
                    "L'expert assigné n'a pas répondu dans les délais.\n\n" +
                    "⚡ Système en action :\n" +
                    "• Recherche d'un nouvel expert\n" +
                    "• Notification immédiate au nouvel expert\n" +
                    "• Nouveau délai : " + EXPERT_ACTION_RESPONSE_HOURS + " heure(s)\n\n" +
                    "📊 Avancement :\n" +
                    "Experts contactés : " + (req.getExcludedExpertClientIds().size() + 1) + "\n" +
                    "Temps écoulé : " + formatDuration(req.getCreatedAt(), LocalDateTime.now()) + "\n\n" +
                    "⏳ Prochaine mise à jour :\n" +
                    "Dans 15-30 minutes";

            vendeurNotif.put("message", vendeurMessage);
            vendeurNotif.put("notificationType", NOTIF_TYPE_EXPIRED_DEADLINE);
            vendeurNotif.put("productName", req.getProduit().getNom());

            notificationService.processEvent(
                    NotificationType.MESSAGE,
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

                // NOTIFICATION AMÉLIORÉE À L'EXPERT
                Map<String, Object> expertNotif = new HashMap<>();

                String expertMessage = "⏰ Délai de rapport dépassé\n\n" +
                        "Produit : " + req.getProduit().getNom() + "\n" +
                        "Vendeur : " + req.getVendeur().getPrenom() + " " + req.getVendeur().getNom() + "\n" +
                        "Délai initial : " + formatDateTime(req.getReportSubmissionDeadline()) + "\n\n" +
                        "⚠️ Conséquences :\n" +
                        "• Vous avez été retiré de cette expertise\n" +
                        "• La demande a été réassignée\n" +
                        "• Cela affecte votre réputation d'expert\n\n" +
                        "💡 Pour éviter cela :\n" +
                        "1. Planifiez vos expertises\n" +
                        "2. Utilisez les rappels\n" +
                        "3. Refusez les demandes si surchargé";

                expertNotif.put("message", expertMessage);
                expertNotif.put("notificationType", NOTIF_TYPE_REPORT_DEADLINE_EXPIRED);
                expertNotif.put("productName", req.getProduit().getNom());

                notificationService.processEvent(
                        NotificationType.MESSAGE,
                        Set.of(expertClientId),
                        expertNotif
                );
            }

            // NOTIFICATION AMÉLIORÉE AU VENDEUR
            Map<String, Object> vendeurNotif = new HashMap<>();

            String vendeurMessage = "🔄 Changement d'expert\n\n" +
                    "Produit : " + req.getProduit().getNom() + "\n" +
                    "Expert précédent : " + (req.getExpert() != null && req.getExpert().getClient() != null
                    ? req.getExpert().getClient().getPrenom() + " " + req.getExpert().getClient().getNom()
                    : "Non spécifié") + "\n\n" +
                    "📋 Statut :\n" +
                    "L'expert n'a pas soumis le rapport dans les délais.\n\n" +
                    "⚡ Système automatique :\n" +
                    "• Nouvel expert en cours de contact\n" +
                    "• Délai additionnel : " + REPORT_SUBMISSION_DAYS + " jour(s)\n" +
                    "• Pas de frais supplémentaires\n\n" +
                    "📞 Support :\n" +
                    "Pour toute question, contactez-nous à support@bazart.com";

            vendeurNotif.put("message", vendeurMessage);
            vendeurNotif.put("notificationType", NOTIF_TYPE_REPORT_DEADLINE_EXPIRED);
            vendeurNotif.put("productName", req.getProduit().getNom());

            notificationService.processEvent(
                    NotificationType.MESSAGE,
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

        // NOTIFICATION AMÉLIORÉE AU VENDEUR
        Map<String, Object> data = new HashMap<>();

        String message = "🔄 Expertise relancée\n\n" +
                "Produit : \"" + produit.getNom() + "\"\n" +
                "Catégorie : " + (produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Général") + "\n\n" +
                "✅ Informations supplémentaires enregistrées\n\n" +
                "📋 Nouveau processus :\n" +
                "1. Recherche d'un nouvel expert\n" +
                "2. Acceptation/refus de l'expert\n" +
                "3. Expertise du produit\n" +
                "4. Rapport final\n\n" +
                "⏰ Délai estimé : 24-48 heures\n\n" +
                "📞 Support :\n" +
                "Pour toute question, contactez-nous à support@bazart.com";

        data.put("message", message);
        data.put("notificationType", "REOPEN_AFTER_INFO");
        data.put("productName", produit.getNom());

        notificationService.processEvent(NotificationType.MESSAGE, Set.of(req.getVendeur().getIdclient()), data);

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
    @Override
    @Transactional(readOnly = true)
    public List<ExpertiseRequestDTO> getExpertisedProductsForExpert(Long clientId) {
        Expert expert = expertRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Expert non trouvé"));

        // Récupérer UNIQUEMENT les demandes EXPERTISÉES (avec rapport validé)
        List<ExpertiseRequest> expertisedRequests = expertiseRequestRepository
                .findByExpertAndStatus(expert, ExpertiseStatus.EXPERTISED);

        // Vérifier que chaque demande a bien un rapport associé
        List<ExpertiseRequest> verifiedRequests = expertisedRequests.stream()
                .filter(req -> {
                    Optional<ExpertiseReport> report = expertiseReportRepository
                            .findByExpertiseRequestId(req.getId());
                    return report.isPresent();
                })
                .collect(Collectors.toList());

        // Convertir en DTO et trier par date décroissante
        return verifiedRequests.stream()
                .map(this::toDto)
                .sorted((a, b) -> {
                    try {
                        ExpertiseRequest reqA = expertiseRequestRepository.findById(a.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));
                        ExpertiseRequest reqB = expertiseRequestRepository.findById(b.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));
                        return reqB.getCreatedAt().compareTo(reqA.getCreatedAt());
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ExpertiseRequestDTO> getInProgressExpertisesForExpert(Long clientId) {
        Expert expert = expertRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Expert non trouvé"));

        // Récupérer uniquement les PLANNED qui n'ont pas encore de rapport
        List<ExpertiseRequest> plannedRequests = expertiseRequestRepository
                .findByExpertAndStatus(expert, ExpertiseStatus.PLANNED);

        return plannedRequests.stream()
                .filter(req -> {
                    Optional<ExpertiseReport> report = expertiseReportRepository
                            .findByExpertiseRequestId(req.getId());
                    return report.isEmpty(); // Pas encore de rapport
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

}