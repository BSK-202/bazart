package com.marketplace.expertise.repository;

import com.marketplace.expertise.entity.ExpertiseRequest;
import com.marketplace.expertise.entity.ExpertiseStatus;
import com.marketplace.user.entity.Expert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<ExpertiseRequest> findByStatusIn(List<ExpertiseStatus> statuses);
    List<ExpertiseRequest> findByExpert(Expert expert);

    // Ou plus spécifiquement pour les expertises terminées avec rapport
    @Query("SELECT er FROM ExpertiseRequest er WHERE er.expert = :expert " +
            "AND er.status = :status " +
            "ORDER BY er.createdAt DESC")
    List<ExpertiseRequest> findCompletedExpertisesByExpert(
            @Param("expert") Expert expert,
            @Param("status") ExpertiseStatus status);
}