package com.marketplace.catalog.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
public class ProduitDTO {
    private Long id;
    private String nom;
    private String description;
    private Double prixDebut;
    private Double prixFin;
    private String etat; //  MODIFIÉ : remplace isVerifier
    private String vendeurNom;
    private String acheteurNom;
    private Long acheteurId;
    private String categorieNom;
    private int nombreInteractions;
    private int nombreCommentaires;
    private List<String> images;
    private Long categorieId;    // Pour la création
    private Long vendeurId;
    private String dateenchere;
    private Integer dureeEnchereJours;
    private Long domaineId;

    //---------------------attributs et methodes d'expertisation--------------------------
    @JsonProperty("aExpertise")
    private boolean aExpertise;
    @JsonProperty("expertiseMethod")
    private String expertiseMethod; // "ONLINE" ou "ONSITE"
    @JsonProperty("expertiseSlot1")
    private String expertiseSlot1; // ISO string "yyyy-MM-dd'T'HH:mm"
    @JsonProperty("expertiseSlot2")
    private String expertiseSlot2;
    @JsonProperty("expertiseSlot3")
    private String expertiseSlot3;

    private String expertisePublicComment;
    private String expertiseAuthenticityLevel;
    private String expertiseProductCondition;
    private boolean expertiseApproved;

    private Long expertiseRequestId;

    public Long getExpertiseRequestId() {
        return expertiseRequestId;
    }

    public void setExpertiseRequestId(Long expertiseRequestId) {
        this.expertiseRequestId = expertiseRequestId;
    }


    public String getExpertiseMethod() {
        return expertiseMethod;
    }

    public void setExpertiseMethod(String expertiseMethod) {
        this.expertiseMethod = expertiseMethod;
    }

    public String getExpertiseSlot1() {
        return expertiseSlot1;
    }
    public void setExpertiseSlot1(String expertiseSlot1) {
        this.expertiseSlot1 = expertiseSlot1;
    }

    public String getExpertiseSlot2() {
        return expertiseSlot2;
    }

    public void setExpertiseSlot2(String expertiseSlot2) {
        this.expertiseSlot2 = expertiseSlot2;
    }

    public String getExpertiseSlot3() {
        return expertiseSlot3;
    }

    public void setExpertiseSlot3(String expertiseSlot3) {
        this.expertiseSlot3 = expertiseSlot3;
    }







    public String getExpertisePublicComment() {
        return expertisePublicComment;
    }

    public void setExpertisePublicComment(String expertisePublicComment) {
        this.expertisePublicComment = expertisePublicComment;
    }


    public String getExpertiseAuthenticityLevel() {
        return expertiseAuthenticityLevel;
    }

    public void setExpertiseAuthenticityLevel(String expertiseAuthenticityLevel) {
        this.expertiseAuthenticityLevel = expertiseAuthenticityLevel;
    }


    public String getExpertiseProductCondition() {
        return expertiseProductCondition;
    }

    public void setExpertiseProductCondition(String expertiseProductCondition) {
        this.expertiseProductCondition = expertiseProductCondition;
    }

    public boolean isExpertiseApproved() { return expertiseApproved ; }
    public void setExpertiseApproved(boolean expertiseApproved) { this.expertiseApproved = expertiseApproved; }


    //------------------------------------------------------------------------------------

    // getters et setters
    public Long getAcheteurId() {
        return acheteurId;
    }

    public void setAcheteurId(Long acheteurId) {
        this.acheteurId = acheteurId;
    }
    public Long getDomaineId() { return domaineId; }
    public void setDomaineId(Long domaineId) { this.domaineId = domaineId; }
    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrixDebut() { return prixDebut; }
    public void setPrixDebut(Double prixDebut) { this.prixDebut = prixDebut; }
    public String getDatepublication() {
        return datepublication;
    }

    public void setDatepublication(String datepublication) {
        this.datepublication = datepublication;
    }

    private String datepublication;
    public Double getPrixFin() { return prixFin; }
    public void setPrixFin(Double prixFin) { this.prixFin = prixFin; }
    public String getDateenchere() {
        return dateenchere;
    }

    public void setDateenchere(String dateenchere) {
        this.dateenchere = dateenchere;
    }

    //  NOUVEAU
    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public String getVendeurNom() { return vendeurNom; }
    public void setVendeurNom(String vendeurNom) { this.vendeurNom = vendeurNom; }

    public String getAcheteurNom() { return acheteurNom; }
    public void setAcheteurNom(String acheteurNom) { this.acheteurNom = acheteurNom; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public int getNombreInteractions() { return nombreInteractions; }
    public void setNombreInteractions(int nombreInteractions) { this.nombreInteractions = nombreInteractions; }

    public int getNombreCommentaires() { return nombreCommentaires; }
    public void setNombreCommentaires(int nombreCommentaires) { this.nombreCommentaires = nombreCommentaires; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public Long getVendeurId() { return vendeurId; }
    public void setVendeurId(Long vendeurId) { this.vendeurId = vendeurId; }
    public boolean isAExpertise() { return aExpertise; }
    public void setAExpertise(boolean aExpertise) { this.aExpertise = aExpertise; }

    public Integer getDureeEnchereJours() {
        return dureeEnchereJours;
    }

    public void setDureeEnchereJours(Integer dureeEnchereJours) {
        this.dureeEnchereJours = dureeEnchereJours;
    }
}