package com.marketplace.interaction.entity;



import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Interaction")
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idinteraction;

    private LocalDateTime date;

    @ManyToOne
    @JoinColumn(name = "idproduit", referencedColumnName = "idproduit")
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "idclient", referencedColumnName = "idclient")
    private Client client;

    // Getters et Setters
    public Long getIdinteraction() { return idinteraction; }
    public void setIdinteraction(Long idinteraction) { this.idinteraction = idinteraction; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
}