package com.marketplace.expertise.service;

import com.marketplace.expertise.entity.ExpertiseReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface ExpertiseReportService {
    ExpertiseReport createReportAndGeneratePDF(
            Long expertiseRequestId,
            Long expertId,
            // AUTHENTICITÉ
            String authenticityLevel,
            Integer authenticityConfidence,
            String authenticityProof,
            // ÉTAT DU PRODUIT
            String productCondition,
            Integer conditionScore,
            String visualCondition,
            String functionalCondition,
            Boolean conformityDescription,
            // DESCRIPTION DÉTAILLÉE
            String detailedDescription,
            String testsPerformed,
            // ESTIMATION
            Double recommendedStartPrice,
            String priceJustification,
            // RECOMMANDATIONS
            String recommendation,
            String saleRecommendations,
            String buyerWarnings,
            // DOCUMENT
            MultipartFile uploadedDocument
    );

    Optional<ExpertiseReport> findByExpertiseRequestId(Long requestId);
}