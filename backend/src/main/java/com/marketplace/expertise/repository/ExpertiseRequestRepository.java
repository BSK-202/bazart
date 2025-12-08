package com.marketplace.expertise.repository;

import com.marketplace.expertise.entity.ExpertiseRequest;
import com.marketplace.expertise.entity.ExpertiseStatus;
import com.marketplace.user.entity.Expert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExpertiseRequestRepository extends JpaRepository<ExpertiseRequest, Long> {

    Optional<ExpertiseRequest> findFirstByProduitIdproduitOrderByCreatedAtDesc(Long produitId);

    List<ExpertiseRequest> findByExpertAndStatus(Expert expert, ExpertiseStatus status);

    List<ExpertiseRequest> findByExpertAndStatusIn(Expert expert, List<ExpertiseStatus> statuses);

    List<ExpertiseRequest> findByStatusAndExpertResponseDeadlineBefore(ExpertiseStatus status, LocalDateTime now);

    // --- ADD THIS FOR THE 3-DAY REPORT DEADLINE EXPIRATION ---
    List<ExpertiseRequest> findByStatusAndReportSubmissionDeadlineBefore(ExpertiseStatus status, LocalDateTime now);
}