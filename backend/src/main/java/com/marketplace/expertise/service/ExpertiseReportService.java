package com.marketplace.expertise.service;

import com.marketplace.expertise.entity.ExpertiseReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface ExpertiseReportService {
    ExpertiseReport createReportAndGeneratePDF(
            Long expertiseRequestId,
            Long expertId,
            String productCondition,
            String authenticityLevel,
            Double estimatedMinPrice,
            Double estimatedMaxPrice,
            Double recommendedStartPrice,
            String recommendation,
            String commentsPublic,
            String commentsInternal,
            MultipartFile uploadedDocument
    );
    Optional<ExpertiseReport> findByExpertiseRequestId(Long requestId);
}