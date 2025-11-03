package com.marketplace.Enchere.entity;



import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enchere")
public class Enchere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEnchere;

    @ManyToOne
    @JoinColumn(name = "idclient", nullable = false)
    private Client encherisseur;

    @ManyToOne
    @JoinColumn(name = "idproduit", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDateTime dateEnchere;

    // Constructeurs
    public Enchere() {}

    public Enchere(Client encherisseur, Produit produit, Double montant) {
        this.encherisseur = encherisseur;
        this.produit = produit;
        this.montant = montant;
        this.dateEnchere = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getIdEnchere() { return idEnchere; }
    public void setIdEnchere(Long idEnchere) { this.idEnchere = idEnchere; }

    public Client getEncherisseur() { return encherisseur; }
    public void setEncherisseur(Client encherisseur) { this.encherisseur = encherisseur; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public LocalDateTime getDateEnchere() { return dateEnchere; }
    public void setDateEnchere(LocalDateTime dateEnchere) { this.dateEnchere = dateEnchere; }
}