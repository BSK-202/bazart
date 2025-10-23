package com.marketplace.interaction.entity;



import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Commentaire")
public class Commentaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idcommentaire;

    private String contenu;
    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "idproduit", referencedColumnName = "idproduit")
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "idclient", referencedColumnName = "idclient")
    private Client client;

    // Getters et Setters
    public Long getIdcommentaire() { return idcommentaire; }
    public void setIdcommentaire(Long idcommentaire) { this.idcommentaire = idcommentaire; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}
