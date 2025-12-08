package com.marketplace.expertise.config;

import com.marketplace.expertise.service.ExpertiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpertiseScheduler {

    private final ExpertiseService expertiseService;

    // Toutes les 30 minutes, par exemple
    @Scheduled(fixedDelay = 30 * 60 * 1000)
    public void checkExpiredRequests() {
        expertiseService.processExpiredExpertDecisions();
        expertiseService.processExpiredReportSubmissions();
    }
}