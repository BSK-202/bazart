package com.marketplace.expertise.entity;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import com.marketplace.user.entity.Expert;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "expertise_request")
public class ExpertiseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Produit concerné
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    // Vendeur
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private Client vendeur;

    // Expert actuellement sollicité
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id")
    private Expert expert;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private ExpertiseMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpertiseStatus status = ExpertiseStatus.CREATED;

    // slots proposés par le vendeur
    @OneToMany(mappedBy = "expertiseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpertiseSlot> slots = new ArrayList<>();

    // date de création + date d'expiration de la réponse expert
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @ElementCollection
    @CollectionTable(name = "expertise_request_exclusions",
            joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "expert_client_id")
    private Set<Long> excludedExpertClientIds = new HashSet<>();


    // prix
    @Column(name = "price")
    private Double price;

    // date/heure finalement confirmée
    @Column(name = "confirmed_datetime")
    private LocalDateTime confirmedDateTime;

    @Column(name = "location")
    private String location;

    @Column(name = "expert_response_deadline")
    private LocalDateTime expertResponseDeadline; //createdAt + 24h

    @Column(name = "report_submission_deadline")
    private LocalDateTime reportSubmissionDeadline; //deadline pour envoyer le rapport



    public Set<Long> getExcludedExpertClientIds() {
        return excludedExpertClientIds;
    }

    public void setExcludedExpertClientIds(Set<Long> excludedExpertClientIds) {
        this.excludedExpertClientIds = excludedExpertClientIds;
    }

    public LocalDateTime getReportSubmissionDeadline() { return reportSubmissionDeadline; }
    public void setReportSubmissionDeadline(LocalDateTime reportSubmissionDeadline) { this.reportSubmissionDeadline = reportSubmissionDeadline; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getId() {
        return id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Client getVendeur() {
        return vendeur;
    }

    public void setVendeur(Client vendeur) {
        this.vendeur = vendeur;
    }

    public Expert getExpert() {
        return expert;
    }

    public void setExpert(Expert expert) {
        this.expert = expert;
    }

    public ExpertiseMethod getMethod() {
        return method;
    }

    public void setMethod(ExpertiseMethod method) {
        this.method = method;
    }

    public ExpertiseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpertiseStatus status) {
        this.status = status;
    }

    public List<ExpertiseSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<ExpertiseSlot> slots) {
        this.slots = slots;
    }

    public void addSlot(ExpertiseSlot slot) {
        slot.setExpertiseRequest(this);
        this.slots.add(slot);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpertResponseDeadline() {
        return expertResponseDeadline;
    }

    public void setExpertResponseDeadline(LocalDateTime expertResponseDeadline) {
        this.expertResponseDeadline = expertResponseDeadline;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public LocalDateTime getConfirmedDateTime() {
        return confirmedDateTime;
    }

    public void setConfirmedDateTime(LocalDateTime confirmedDateTime) {
        this.confirmedDateTime = confirmedDateTime;
    }
}