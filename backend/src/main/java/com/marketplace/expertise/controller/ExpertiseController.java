package com.marketplace.expertise.controller;

import com.marketplace.expertise.dto.ExpertiseRequestDTO;
import com.marketplace.expertise.service.ExpertiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur principal pour la gestion des demandes d’expertise
 * - Liste des demandes en attente pour un expert
 * - Acceptation/refus par expert
 * - (Éventuellement: récupération détail demande, etc.)
 */
@RestController
@RequestMapping("/api/expertise")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    // Liste des demandes en attente pour un expert (cartes)
    @GetMapping("/expert/{expertId}/pending")
    public ResponseEntity<List<ExpertiseRequestDTO>> getPendingForExpert(@PathVariable Long expertId) {
        return ResponseEntity.ok(expertiseService.getPendingRequestsForExpert(expertId));
    }

    // Expert accepte + choisit un des 3 slots (obligatoire seulement pour ONSITE)
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ExpertiseRequestDTO> acceptRequest(
            @PathVariable Long requestId,
            @RequestParam Long expertId,
            @RequestParam(required = false) Integer slotIndex) {

        return ResponseEntity.ok(
                expertiseService.acceptRequestAndChooseSlot(requestId, expertId, slotIndex)
        );
    }

    // ✅ Ajout : endpoint pour refus d'une demande par l'expert
    @PostMapping("/requests/{requestId}/refuse")
    public ResponseEntity<ExpertiseRequestDTO> refuseRequest(
            @PathVariable Long requestId,
            @RequestParam Long expertId) {

        return ResponseEntity.ok(
                expertiseService.refuseExpertiseRequest(requestId, expertId)
        );
    }
    @GetMapping("/requests/{requestId}/can-submit-report")
    public ResponseEntity<Boolean> canSubmitReport(@PathVariable Long requestId) {
        return ResponseEntity.ok(expertiseService.canSubmitReport(requestId));
    }
    // Optionnel : permet de récupérer une demande précise (pour un détail ou countdown)
    @GetMapping("/requests/{requestId}/detail")
    public ResponseEntity<ExpertiseRequestDTO> getRequestDetail(@PathVariable Long requestId) {
        // Supposons une méthode getRequestDetailById(requestId) à ajouter côté service
        ExpertiseRequestDTO dto = expertiseService.getRequestDetailById(requestId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/requests/{requestId}/reopen-after-info")
    public ResponseEntity<ExpertiseRequestDTO> reopenAfterInfo(@PathVariable Long requestId) {
        return ResponseEntity.ok(expertiseService.reopenAfterSellerInfo(requestId));
    }

    // Liste des demandes assignées (pending + planned)
    @GetMapping("/expert/{expertId}/assigned")
    public ResponseEntity<List<ExpertiseRequestDTO>> getAssignedForExpert(@PathVariable Long expertId) {
        return ResponseEntity.ok(expertiseService.getAssignedRequestsForExpert(expertId));
    }

    @GetMapping("/expert/{expertId}/completed")
    public ResponseEntity<List<ExpertiseRequestDTO>> getCompletedExpertises(
            @PathVariable Long expertId) {
        return ResponseEntity.ok(
                expertiseService.getExpertisedProductsForExpert(expertId)
        );
    }
    @GetMapping("/expert/{expertId}/in-progress")
    public ResponseEntity<List<ExpertiseRequestDTO>> getInProgressExpertises(
            @PathVariable Long expertId) {
        return ResponseEntity.ok(
                expertiseService.getInProgressExpertisesForExpert(expertId)
        );
    }
    @GetMapping("/expert/{expertId}/all-classified")
    public ResponseEntity<Map<String, List<ExpertiseRequestDTO>>> getAllClassifiedForExpert(
            @PathVariable Long expertId) {

        Map<String, List<ExpertiseRequestDTO>> result = new HashMap<>();

        // Pending
        result.put("pending", expertiseService.getPendingRequestsForExpert(expertId));

        // In Progress (PLANNED)
        result.put("inProgress", expertiseService.getInProgressExpertisesForExpert(expertId));

        // Completed
        result.put("completed", expertiseService.getExpertisedProductsForExpert(expertId));

        return ResponseEntity.ok(result);
    }


}