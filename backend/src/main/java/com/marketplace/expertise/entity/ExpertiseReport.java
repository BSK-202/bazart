package com.marketplace.expertise.entity;

import com.marketplace.user.entity.Expert;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "expertise_report")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition")
    private ProductCondition productCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "authenticity_level")
    private AuthenticityLevel authenticityLevel;

    @Column(name = "estimated_min_price")
    private Double estimatedMinPrice;

    @Column(name = "estimated_max_price")
    private Double estimatedMaxPrice;

    @Column(name = "recommended_start_price")
    private Double recommendedStartPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false)
    private ExpertiseRecommendation recommendation;

    @Column(name = "comments_public", columnDefinition = "TEXT")
    private String commentsPublic;

    @Column(name = "comments_internal", columnDefinition = "TEXT")
    private String commentsInternal;

    @Column(name = "report_pdf_path")
    private String reportPdfPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters / setters ...
    public Long getId() {
        return id;
    }

    public ExpertiseRequest getExpertiseRequest() {
        return expertiseRequest;
    }

    public void setExpertiseRequest(ExpertiseRequest expertiseRequest) {
        this.expertiseRequest = expertiseRequest;
    }

    public Expert getExpert() {
        return expert;
    }

    public void setExpert(Expert expert) {
        this.expert = expert;
    }

    public ProductCondition getProductCondition() {
        return productCondition;
    }

    public void setProductCondition(ProductCondition productCondition) {
        this.productCondition = productCondition;
    }

    public AuthenticityLevel getAuthenticityLevel() {
        return authenticityLevel;
    }

    public void setAuthenticityLevel(AuthenticityLevel authenticityLevel) {
        this.authenticityLevel = authenticityLevel;
    }

    public Double getEstimatedMinPrice() {
        return estimatedMinPrice;
    }

    public void setEstimatedMinPrice(Double estimatedMinPrice) {
        this.estimatedMinPrice = estimatedMinPrice;
    }

    public Double getEstimatedMaxPrice() {
        return estimatedMaxPrice;
    }

    public void setEstimatedMaxPrice(Double estimatedMaxPrice) {
        this.estimatedMaxPrice = estimatedMaxPrice;
    }

    public Double getRecommendedStartPrice() {
        return recommendedStartPrice;
    }

    public void setRecommendedStartPrice(Double recommendedStartPrice) {
        this.recommendedStartPrice = recommendedStartPrice;
    }

    public ExpertiseRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(ExpertiseRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getCommentsPublic() {
        return commentsPublic;
    }

    public void setCommentsPublic(String commentsPublic) {
        this.commentsPublic = commentsPublic;
    }

    public String getCommentsInternal() {
        return commentsInternal;
    }

    public void setCommentsInternal(String commentsInternal) {
        this.commentsInternal = commentsInternal;
    }

    public String getReportPdfPath() {
        return reportPdfPath;
    }

    public void setReportPdfPath(String reportPdfPath) {
        this.reportPdfPath = reportPdfPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}