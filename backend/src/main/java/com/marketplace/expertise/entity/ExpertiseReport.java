package com.marketplace.expertise.entity;

import com.marketplace.user.entity.Expert;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "expertise_report")
@Getter
@Setter
public class ExpertiseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expertise_request_id", nullable = false)
    private ExpertiseRequest expertiseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private Expert expert;

    // ========== AUTHENTICITÉ ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "authenticity_level")
    private AuthenticityLevel authenticityLevel;

    @Column(name = "authenticity_confidence")
    private Integer authenticityConfidence;

    @Column(name = "authenticity_proof", columnDefinition = "TEXT")
    private String authenticityProof;

    // ========== ÉTAT DU PRODUIT ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition")
    private ProductCondition productCondition;

    @Column(name = "condition_score")
    private Integer conditionScore;

    @Column(name = "visual_condition", columnDefinition = "TEXT")
    private String visualCondition;

    @Column(name = "functional_condition", columnDefinition = "TEXT")
    private String functionalCondition;

    @Column(name = "conformity_description")
    private Boolean conformityDescription;

    // ========== DESCRIPTION DÉTAILLÉE ==========
    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;

    @Column(name = "tests_performed", columnDefinition = "TEXT")
    private String testsPerformed;

    // ========== ESTIMATION ==========
    @Column(name = "recommended_start_price")
    private Double recommendedStartPrice;

    @Column(name = "price_justification", columnDefinition = "TEXT")
    private String priceJustification;

    // ========== RECOMMANDATIONS ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false)
    private ExpertiseRecommendation recommendation;

    @Column(name = "sale_recommendations", columnDefinition = "TEXT")
    private String saleRecommendations;

    @Column(name = "buyer_warnings", columnDefinition = "TEXT")
    private String buyerWarnings;

    // ========== MÉTADONNÉES ==========
    @Column(name = "report_pdf_path")
    private String reportPdfPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}