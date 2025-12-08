package com.marketplace.expertise.repository;

import com.marketplace.expertise.entity.ExpertiseReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpertiseReportRepository extends JpaRepository<ExpertiseReport, Long> {

    Optional<ExpertiseReport> findByExpertiseRequestId(Long expertiseRequestId);
}