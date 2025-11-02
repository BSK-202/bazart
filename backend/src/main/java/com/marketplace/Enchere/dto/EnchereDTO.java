package com.marketplace.Enchere.dto;


import java.time.LocalDateTime;

public class EnchereDTO {
    private Long idEnchere;
    private Long encherisseurId;
    private String encherisseurNom;
    private String encherisseurPrenom;
    private Long produitId;
    private String produitNom;
    private Double montant;
    private LocalDateTime dateEnchere;
    private Boolean isLeading;

    // Getters et Setters
    public Long getIdEnchere() { return idEnchere; }
    public void setIdEnchere(Long idEnchere) { this.idEnchere = idEnchere; }

    public Long getEncherisseurId() { return encherisseurId; }
    public void setEncherisseurId(Long encherisseurId) { this.encherisseurId = encherisseurId; }

    public String getEncherisseurNom() { return encherisseurNom; }
    public void setEncherisseurNom(String encherisseurNom) { this.encherisseurNom = encherisseurNom; }

    public String getEncherisseurPrenom() { return encherisseurPrenom; }
    public void setEncherisseurPrenom(String encherisseurPrenom) { this.encherisseurPrenom = encherisseurPrenom; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public LocalDateTime getDateEnchere() { return dateEnchere; }
    public void setDateEnchere(LocalDateTime dateEnchere) { this.dateEnchere = dateEnchere; }

    public Boolean getIsLeading() { return isLeading; }
    public void setIsLeading(Boolean isLeading) { this.isLeading = isLeading; }
}