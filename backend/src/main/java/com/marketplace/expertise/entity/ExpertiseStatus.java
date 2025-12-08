package com.marketplace.expertise.entity;

public enum ExpertiseStatus {
    CREATED,
    PENDING_EXPERT_DECISION,  // attente réponse expert (24h)
    PLANNED,
    NO_EXPERT_AVAILABLE,
    CANCELLED,
    EXPERTISED
}