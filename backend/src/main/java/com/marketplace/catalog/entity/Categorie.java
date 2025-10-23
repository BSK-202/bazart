package com.marketplace.catalog.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categorie")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idcategorie")
    private Long idCategorie;

    @Column(name = "nomcategorie")
    private String nomCategorie;

    @Column(name = "description")
    private String description;

    @Column(name = "image")
    private String image;

    // Relation avec Domaine
    @ManyToOne
    @JoinColumn(name = "iddomaine", nullable = false)
    private Domaine domaine;

    // Getters & Setters
    public Long getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(Long idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
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

    public Domaine getDomaine() {
        return domaine;
    }

    public void setDomaine(Domaine domaine) {
        this.domaine = domaine;
    }


}