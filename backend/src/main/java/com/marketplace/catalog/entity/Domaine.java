package com.marketplace.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "domaine")
public class Domaine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrément
    @Column(name = "iddomaine")
    private Long idDomaine;

    @Column(name = "nomdomaine")
    private String nomDomaine;

    @Column(name = "description")
    private String description;

    @Column(name = "image")
    private String image;

    // Getters et Setters
    public Long getIdDomaine() {
        return idDomaine;
    }

    public void setIdDomaine(Long idDomaine) {
        this.idDomaine = idDomaine;
    }

    public String getNomDomaine() {
        return nomDomaine;
    }

    public void setNomDomaine(String nomDomaine) {
        this.nomDomaine = nomDomaine;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
