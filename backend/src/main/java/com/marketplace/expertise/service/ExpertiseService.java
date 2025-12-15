package com.marketplace.expertise.service;

import com.marketplace.expertise.dto.ExpertiseRequestDTO;

import java.util.List;
import java.util.Map;

public interface ExpertiseService {

    void createRequestAfterProductAccepted(Long produitId);

    List<ExpertiseRequestDTO> getPendingRequestsForExpert(Long expertId);

    ExpertiseRequestDTO acceptRequestAndChooseSlot(Long requestId, Long expertId, Integer slotIndex);

    void processExpiredExpertDecisions();
    ExpertiseRequestDTO refuseExpertiseRequest(Long requestId, Long expertId);

    // méthode appelée pour soumettre le rapport côté reportService
    void processExpiredReportSubmissions(); // pour gérer deadline 3j rapport
    boolean canSubmitReport(Long requestId);  // Ajoutez cette ligne

    public ExpertiseRequestDTO getRequestDetailById(Long requestId);
    public Map<String, List<ExpertiseRequestDTO>> getAllClassifiedForExpert(Long clientId) ;

    ExpertiseRequestDTO reopenAfterSellerInfo(Long requestId);
    List<ExpertiseRequestDTO> getExpertisedProductsForExpert(Long clientId);
    public List<ExpertiseRequestDTO> getInProgressExpertisesForExpert(Long clientId) ;

    public void retryBlockedRequests();
    public List<ExpertiseRequestDTO> getAssignedRequestsForExpert(Long clientId);
}