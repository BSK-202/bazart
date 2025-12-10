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
import java.util.*;

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
    private static final int EXPERT_ACTION_RESPONSE_HOURS = 24;   // Temps pour accepter/refuser la première demande
    private static final int REPORT_SUBMISSION_DAYS = 3;           // Nombre de jours pour soumettre le rapport après acceptation

    private static final double EXPERT_PAYOUT_RATE = 0.7; // 70% pour l’expert
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

        // Deadline pour accepter/refuser la demande (24h)
        request.setExpertResponseDeadline(LocalDateTime.now().plusHours(EXPERT_ACTION_RESPONSE_HOURS));

        // Location précisée si sur place
        if (request.getMethod() == ExpertiseMethod.ONSITE) {
            request.setLocation("Magasin Bazart, Avenue XXX, Ville YYY");
        } else {
            request.setLocation("ONLINE");
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

    // Implémentation avec exclusion
    private void assignExpertAndNotify(ExpertiseRequest request, Set<Long> excludeIds) {
        Produit produit = request.getProduit();
        if (produit.getCategorie() == null || produit.getCategorie().getDomaine() == null) return;

        Long domaineId = produit.getCategorie().getDomaine().getIdDomaine();

        // Experts actifs du domaine
        List<Expert> expertsDomain = expertRepository.findByDomaineId(domaineId).stream()
                .filter(Expert::isActive)
                .toList();

        if (expertsDomain.isEmpty()) {
            request.setStatus(ExpertiseStatus.NO_EXPERT_AVAILABLE);
            expertiseRequestRepository.save(request);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Aucun expert n'est disponible pour le produit \"" + produit.getNom() + "\".");
            notificationService.processEvent(NotificationType.GENERIC, Set.of(request.getVendeur().getIdclient()), data);
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
            request.setStatus(ExpertiseStatus.NO_EXPERT_AVAILABLE);
            expertiseRequestRepository.save(request);
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Aucun expert n'est disponible pour le produit \"" + produit.getNom() + "\".");
            notificationService.processEvent(NotificationType.GENERIC, Set.of(request.getVendeur().getIdclient()), data);
            return;
        }

        // Choisir l’expert le moins chargé
        Expert chosen = candidates.stream()
                .min(Comparator.comparingInt(Expert::getNombreProduitsExpertise))
                .orElseThrow();

        // Assignation
        request.setExpert(chosen);
        request.setStatus(ExpertiseStatus.PENDING_EXPERT_DECISION);
        request.setExpertResponseDeadline(LocalDateTime.now().plusHours(EXPERT_ACTION_RESPONSE_HOURS));
        expertiseRequestRepository.save(request);

        // Notification à l’expert
        if (chosen.getClient() != null) {
            Long expertUserId = chosen.getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();
            expertNotif.put("productName", produit.getNom());
            expertNotif.put("expertiseMethod", request.getMethod().name());
            expertNotif.put("productId", produit.getIdproduit());
            expertNotif.put("message",
                    "Vous avez une nouvelle demande d'expertise sur le produit \"" + produit.getNom() + "\". " +
                            (request.getMethod() == ExpertiseMethod.ONSITE
                                    ? "Veuillez consulter les créneaux proposés et accepter/refuser."
                                    : "Veuillez accepter/refuser pour expertise en ligne depuis votre espace expert.")
            );
            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);
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

        // Vérifier sur l’ID client de l’expert
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
            // Deadline rapport : 3j après le slot choisi
            request.setReportSubmissionDeadline(chosen.getDateTime().plusDays(REPORT_SUBMISSION_DAYS));
        } else {
            // Online : deadline rapport à partir de l'acceptation
            request.setConfirmedDateTime(null);
            request.setReportSubmissionDeadline(LocalDateTime.now().plusDays(REPORT_SUBMISSION_DAYS));
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
            expertNotif.put("message", "Vous avez accepté l'expertise pour le produit \"" +
                    request.getProduit().getNom() + "\"" +
                    (request.getConfirmedDateTime() != null ? " le " + request.getConfirmedDateTime() : ""));
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

        // Réinitialiser la demande
        request.setExpert(null);
        request.setStatus(ExpertiseStatus.CREATED);
        request.setExpertResponseDeadline(null);
        request.setReportSubmissionDeadline(null);
        expertiseRequestRepository.save(request);

        // Notif vendeur (refus)
        Map<String, Object> vendeurNotif = new HashMap<>();
        vendeurNotif.put("message", "L'expert a refusé la demande d'expertise pour le produit \"" +
                request.getProduit().getNom() + "\". Elle sera réassignée.");
        notificationService.processEvent(NotificationType.GENERIC,
                Set.of(request.getVendeur().getIdclient()), vendeurNotif);

        // Réassignation en excluant tous les experts déjà essayés
        assignExpertAndNotify(request, request.getExcludedExpertClientIds());

        return toDto(request);
    }

    // ==== 6) SCHEDULER POUR TRAITER LES DEADLINES ====
    @Override
    public void processExpiredExpertDecisions() {
        LocalDateTime now = LocalDateTime.now();
        List<ExpertiseRequest> expired = expertiseRequestRepository
                .findByStatusAndExpertResponseDeadlineBefore(ExpertiseStatus.PENDING_EXPERT_DECISION, now);
        for (ExpertiseRequest req : expired) {
            req.setExpert(null);
            req.setStatus(ExpertiseStatus.CREATED);
            req.setExpertResponseDeadline(null);
            expertiseRequestRepository.save(req);
            assignExpertAndNotify(req);
        }
    }

    @Override
    public void processExpiredReportSubmissions() {
        LocalDateTime now = LocalDateTime.now();
        List<ExpertiseRequest> expired = expertiseRequestRepository
                .findByStatusAndReportSubmissionDeadlineBefore(ExpertiseStatus.PLANNED, now);
        for (ExpertiseRequest req : expired) {
            req.setStatus(ExpertiseStatus.CREATED);
            req.setExpert(null);
            req.setReportSubmissionDeadline(null);
            expertiseRequestRepository.save(req);
            assignExpertAndNotify(req);
        }
    }

    // ==== 7) CONVERSION DTO ====
    // ... dans toDto(ExpertiseRequest req)
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
        dto.setExpertShare(expertShare); // part de l’expert

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
        req.setExpertResponseDeadline(LocalDateTime.now().plusHours(EXPERT_ACTION_RESPONSE_HOURS));
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

}