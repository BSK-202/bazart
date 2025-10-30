package com.marketplace.catalog.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.entity.Interaction;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Produit")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idproduit;

    private String nom;
    private String description;

    @Column(name = "prixdebut")
    private Double prixDebut;

    @Column(name = "prixfin")
    private Double prixFin;

    @Column(name = "datepublication")
    private LocalDateTime datePublication;


    // ✅ NOUVEAU : Date de début d'enchère
    @Column(name = "dateenchere")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateEnchere;
    @Column(nullable = false)
    private boolean aExpertise;

    // ✅ NOUVEAU : Attribut etat
    @Column(nullable = true)
    private String etat; // null, "en_attente", "accepter", "refuser"

    @ManyToOne
    @JoinColumn(name = "idclient", referencedColumnName = "idclient")
    private Client vendeur;

    @ManyToOne
    @JoinColumn(name = "idclientacheteur", referencedColumnName = "idclient")
    private Client acheteur;

    @ManyToOne
    @JoinColumn(name = "idcategorie", referencedColumnName = "idcategorie")
    private Categorie categorie;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProduitImage> images;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Interaction> interactions;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentaires;

    // Getters et Setters existants...
    public Long getIdproduit() { return idproduit; }
    public void setIdproduit(Long idproduit) { this.idproduit = idproduit; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrixDebut() { return prixDebut; }
    public void setPrixDebut(Double prixDebut) { this.prixDebut = prixDebut; }

    public Double getPrixFin() { return prixFin; }
    public void setPrixFin(Double prixFin) { this.prixFin = prixFin; }

    public LocalDateTime getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDateTime datePublication) { this.datePublication = datePublication; }

    public boolean isAExpertise() { return aExpertise; }
    public void setAExpertise(boolean aExpertise) { this.aExpertise = aExpertise; }

    // ✅ NOUVEAU : Getter/Setter pour etat
    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public Client getVendeur() { return vendeur; }
    public void setVendeur(Client vendeur) { this.vendeur = vendeur; }

    public Client getAcheteur() { return acheteur; }
    public void setAcheteur(Client acheteur) { this.acheteur = acheteur; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    public List<ProduitImage> getImages() { return images; }
    public void setImages(List<ProduitImage> images) { this.images = images; }

    public List<Interaction> getInteractions() { return interactions; }
    public void setInteractions(List<Interaction> interactions) { this.interactions = interactions; }

    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) { this.commentaires = commentaires; }
    public LocalDateTime getDateEnchere() {
        return dateEnchere;
    }

    public void setDateEnchere(LocalDateTime dateEnchere) {
        this.dateEnchere = dateEnchere;
    }

    public boolean isaExpertise() {
        return aExpertise;
    }

    public void setaExpertise(boolean aExpertise) {
        this.aExpertise = aExpertise;
    }

}