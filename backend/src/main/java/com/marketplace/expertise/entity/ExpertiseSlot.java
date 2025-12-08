package com.marketplace.expertise.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expertise_slot")
public class ExpertiseSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // position 1, 2, 3 (pour les 3 propositions)
    @Column(name = "slot_index", nullable = false)
    private int slotIndex;

    @Column(name = "slot_datetime", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "chosen", nullable = false)
    private boolean chosen = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expertise_request_id", nullable = false)
    private ExpertiseRequest expertiseRequest;

    // getters / setters
    public Long getId() {
        return id;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isChosen() {
        return chosen;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }

    public ExpertiseRequest getExpertiseRequest() {
        return expertiseRequest;
    }

    public void setExpertiseRequest(ExpertiseRequest expertiseRequest) {
        this.expertiseRequest = expertiseRequest;
    }
}