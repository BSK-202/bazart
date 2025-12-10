package com.marketplace.expertise.service;

import com.marketplace.expertise.dto.ExpertiseRequestDTO;

import java.util.List;

public interface ExpertiseService {

    void createRequestAfterProductAccepted(Long produitId);

    List<ExpertiseRequestDTO> getPendingRequestsForExpert(Long expertId);

    ExpertiseRequestDTO acceptRequestAndChooseSlot(Long requestId, Long expertId, Integer slotIndex);

    void processExpiredExpertDecisions();
    ExpertiseRequestDTO refuseExpertiseRequest(Long requestId, Long expertId);

    // méthode appelée pour soumettre le rapport côté reportService
    void processExpiredReportSubmissions(); // pour gérer deadline 3j rapport

    public ExpertiseRequestDTO getRequestDetailById(Long requestId);

    ExpertiseRequestDTO reopenAfterSellerInfo(Long requestId);

    public List<ExpertiseRequestDTO> getAssignedRequestsForExpert(Long clientId);
}