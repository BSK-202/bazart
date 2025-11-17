package com.marketplace.catalog.dto;

import java.util.List;

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
    private boolean aExpertise;
    private String dateenchere;
    private Integer dureeEnchereJours;
    private Long domaineId;
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