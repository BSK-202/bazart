package com.marketplace.expertise.config;

import com.marketplace.expertise.service.ExpertiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpertiseScheduler {

    private final ExpertiseService expertiseService;

    // Exécuter toutes les 5 minutes (300000 ms)
    @Scheduled(fixedDelay = 45000)
    public void processExpiredDeadlines() {
        try {
            expertiseService.processExpiredExpertDecisions();
            expertiseService.processExpiredReportSubmissions();
        } catch (Exception e) {
            // Log l'erreur mais ne pas planter le scheduler
            System.err.println("Erreur dans le scheduler de deadlines: " + e.getMessage());
        }
    }

    // Exécuter toutes les heures (3600000 ms)
    @Scheduled(fixedDelay = 3600000)
    public void retryBlockedRequests() {
        try {
            expertiseService.retryBlockedRequests();
        } catch (Exception e) {
            System.err.println("Erreur dans le scheduler de réessai: " + e.getMessage());
        }
    }

    // Optionnel: Exécuter tous les jours à minuit pour nettoyer les anciennes exclusions
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldExclusions() {
        // Vous pourriez ajouter une méthode pour nettoyer les exclusions de plus d'une semaine
        // expertiseService.cleanupOldExclusions();
    }
}