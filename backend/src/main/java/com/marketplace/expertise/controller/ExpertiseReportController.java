package com.marketplace.expertise.controller;

import com.marketplace.expertise.dto.ExpertiseReportDTO;
import com.marketplace.expertise.entity.ExpertiseReport;
import com.marketplace.expertise.service.ExpertiseReportService;
import com.marketplace.expertise.service.ExpertiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/expertise/requests")
@RequiredArgsConstructor
public class ExpertiseReportController {

    private final ExpertiseReportService reportService;
    private final ExpertiseService expertiseService;

    @PostMapping("/{id}/submit-report")
    public ResponseEntity<?> submitReport(
            @PathVariable("id") Long expertiseRequestId,
            @RequestParam("expertId") Long expertId,

            // AUTHENTICITÉ
            @RequestParam("authenticityLevel") String authenticityLevel,
            @RequestParam(value = "authenticityConfidence", required = false) Integer authenticityConfidence,
            @RequestParam(value = "authenticityProof", required = false) String authenticityProof,

            // ÉTAT DU PRODUIT
            @RequestParam("productCondition") String productCondition,
            @RequestParam(value = "conditionScore", required = false) Integer conditionScore,
            @RequestParam(value = "visualCondition", required = false) String visualCondition,
            @RequestParam(value = "functionalCondition", required = false) String functionalCondition,
            @RequestParam(value = "conformityDescription", required = false) Boolean conformityDescription,

            // DESCRIPTION DÉTAILLÉE
            @RequestParam(value = "detailedDescription", required = false) String detailedDescription,
            @RequestParam(value = "testsPerformed", required = false) String testsPerformed,

            // ESTIMATION
            @RequestParam(value = "recommendedStartPrice", required = false) Double recommendedStartPrice,
            @RequestParam(value = "priceJustification", required = false) String priceJustification,

            // RECOMMANDATIONS
            @RequestParam("recommendation") String recommendation,
            @RequestParam(value = "saleRecommendations", required = false) String saleRecommendations,
            @RequestParam(value = "buyerWarnings", required = false) String buyerWarnings,

            // DOCUMENT
            @RequestPart(value = "document", required = false) MultipartFile document
    ) {
        try {
            // Vérifier si le rapport peut être soumis
            if (!expertiseService.canSubmitReport(expertiseRequestId)) {
                return ResponseEntity.badRequest()
                        .body("Le rapport ne peut pas être soumis maintenant. " +
                                "Pour les expertises sur place, attendez la date du rendez-vous.");
            }

            // Créer le rapport
            ExpertiseReport report = reportService.createReportAndGeneratePDF(
                    expertiseRequestId,
                    expertId,
                    // AUTHENTICITÉ
                    authenticityLevel,
                    authenticityConfidence,
                    authenticityProof,
                    // ÉTAT DU PRODUIT
                    productCondition,
                    conditionScore,
                    visualCondition,
                    functionalCondition,
                    conformityDescription,
                    // DESCRIPTION DÉTAILLÉE
                    detailedDescription,
                    testsPerformed,
                    // ESTIMATION
                    recommendedStartPrice,
                    priceJustification,
                    // RECOMMANDATIONS
                    recommendation,
                    saleRecommendations,
                    buyerWarnings,
                    // DOCUMENT
                    document
            );

            // Mapping entité -> DTO
            ExpertiseReportDTO dto = mapToDTO(report);

            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la soumission du rapport: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/report-pdf")
    public ResponseEntity<?> downloadReportPdf(@PathVariable("id") Long expertiseRequestId) {
        var reportOpt = reportService.findByExpertiseRequestId(expertiseRequestId);
        if (reportOpt.isEmpty() || reportOpt.get().getReportPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = Path.of(reportOpt.get().getReportPdfPath());
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(path.toUri());
            String filename = path.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Erreur lors de la lecture du PDF: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<?> getReport(@PathVariable("id") Long expertiseRequestId) {
        var reportOpt = reportService.findByExpertiseRequestId(expertiseRequestId);
        if (reportOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExpertiseReportDTO dto = mapToDTO(reportOpt.get());
        return ResponseEntity.ok(dto);
    }

    // Méthode utilitaire pour mapper l'entité vers le DTO
    private ExpertiseReportDTO mapToDTO(ExpertiseReport report) {
        ExpertiseReportDTO dto = new ExpertiseReportDTO();

        dto.setId(report.getId());
        dto.setExpertiseRequestId(report.getExpertiseRequest().getId());

        // AUTHENTICITÉ
        dto.setAuthenticityLevel(report.getAuthenticityLevel());
        dto.setAuthenticityConfidence(report.getAuthenticityConfidence());
        dto.setAuthenticityProof(report.getAuthenticityProof());

        // ÉTAT DU PRODUIT
        dto.setProductCondition(report.getProductCondition());
        dto.setConditionScore(report.getConditionScore());
        dto.setVisualCondition(report.getVisualCondition());
        dto.setFunctionalCondition(report.getFunctionalCondition());
        dto.setConformityDescription(report.getConformityDescription());

        // DESCRIPTION DÉTAILLÉE
        dto.setDetailedDescription(report.getDetailedDescription());
        dto.setTestsPerformed(report.getTestsPerformed());

        // ESTIMATION
        dto.setRecommendedStartPrice(report.getRecommendedStartPrice());
        dto.setPriceJustification(report.getPriceJustification());

        // RECOMMANDATIONS
        dto.setRecommendation(report.getRecommendation());
        dto.setSaleRecommendations(report.getSaleRecommendations());
        dto.setBuyerWarnings(report.getBuyerWarnings());

        // MÉTADONNÉES
        dto.setReportPdfPath(report.getReportPdfPath());
        dto.setCreatedAt(report.getCreatedAt());

        return dto;
    }
}