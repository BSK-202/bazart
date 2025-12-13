package com.marketplace.expertise.entity;

public enum ExpertiseStatus {
    CREATED,
    PENDING_EXPERT_DECISION,  // attente réponse expert (24h)
    PLANNED,
    NO_EXPERTS_AVAILABLE,
    CANCELLED,
    EXPERTISED,
    ALL_EXPERTS_TRIED
}