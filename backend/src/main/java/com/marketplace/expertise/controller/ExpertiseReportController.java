package com.marketplace.expertise.controller;

import com.marketplace.expertise.dto.ExpertiseReportDTO;
import com.marketplace.expertise.entity.ExpertiseReport;
import com.marketplace.expertise.service.ExpertiseReportService;
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
public class ExpertiseReportController {

    private final ExpertiseReportService reportService;

    public ExpertiseReportController(ExpertiseReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/{id}/submit-report")
    public ResponseEntity<ExpertiseReportDTO> submitReport(
            @PathVariable("id") Long expertiseRequestId,
            @RequestParam("expertId") Long expertId,
            @RequestParam("productCondition") String productCondition,
            @RequestParam("authenticityLevel") String authenticityLevel,
            @RequestParam(value="estimatedMinPrice", required=false) Double estimatedMinPrice,
            @RequestParam(value="estimatedMaxPrice", required=false) Double estimatedMaxPrice,
            @RequestParam(value="recommendedStartPrice", required=false) Double recommendedStartPrice,
            @RequestParam("recommendation") String recommendation,
            @RequestParam(value="commentsPublic", required = false) String commentsPublic,
            @RequestParam(value="commentsInternal", required = false) String commentsInternal,
            @RequestPart(value="document", required = false) MultipartFile document
    ) {
        var report = reportService.createReportAndGeneratePDF(
                expertiseRequestId,
                expertId,
                productCondition,
                authenticityLevel,
                estimatedMinPrice,
                estimatedMaxPrice,
                recommendedStartPrice,
                recommendation,
                commentsPublic,
                commentsInternal,
                document
        );
        // Mapping entité -> DTO
        ExpertiseReportDTO dto = new ExpertiseReportDTO();
        dto.setExpertiseRequestId(report.getExpertiseRequest().getId());
        dto.setProductCondition(report.getProductCondition());
        dto.setAuthenticityLevel(report.getAuthenticityLevel());
        dto.setEstimatedMinPrice(report.getEstimatedMinPrice());
        dto.setEstimatedMaxPrice(report.getEstimatedMaxPrice());
        dto.setRecommendedStartPrice(report.getRecommendedStartPrice());
        dto.setRecommendation(report.getRecommendation());
        dto.setCommentsPublic(report.getCommentsPublic());
        dto.setCommentsInternal(report.getCommentsInternal());
        return ResponseEntity.ok(dto);
    }


    @GetMapping("/{id}/report-pdf")
    public ResponseEntity<?> downloadReportPdf(@PathVariable("id") Long expertiseRequestId) {
        var reportOpt = reportService.findByExpertiseRequestId(expertiseRequestId);
        if (reportOpt.isEmpty() || reportOpt.get().getReportPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = Path.of(reportOpt.get().getReportPdfPath());
            if (!Files.exists(path)) return ResponseEntity.notFound().build();

            Resource resource = new UrlResource(path.toUri());
            String filename = path.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lecture PDF: " + e.getMessage());
        }
    }
}