package com.marketplace.expertise.dto;

import com.marketplace.expertise.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ExpertiseReportDTO {

    private Long id;
    private Long expertiseRequestId;

    // AUTHENTICITÉ
    private AuthenticityLevel authenticityLevel;
    private Integer authenticityConfidence;
    private String authenticityProof;

    // ÉTAT DU PRODUIT
    private ProductCondition productCondition;
    private Integer conditionScore;
    private String visualCondition;
    private String functionalCondition;
    private Boolean conformityDescription;

    // DESCRIPTION DÉTAILLÉE
    private String detailedDescription;
    private String testsPerformed;

    // ESTIMATION
    private Double recommendedStartPrice;
    private String priceJustification;

    // RECOMMANDATIONS
    private ExpertiseRecommendation recommendation;
    private String saleRecommendations;
    private String buyerWarnings;

    // MÉTADONNÉES
    private String reportPdfPath;
    private LocalDateTime createdAt;
}