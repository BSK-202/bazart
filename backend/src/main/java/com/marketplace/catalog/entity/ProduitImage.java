package com.marketplace.catalog.entity;



import jakarta.persistence.*;

@Entity
@Table(name = "produitimage")
public class ProduitImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idimage;

    private String url;

    @ManyToOne
    @JoinColumn(name = "idproduit", referencedColumnName = "idproduit")
    private Produit produit;

    // Getters et Setters
    public Long getIdimage() { return idimage; }
    public void setIdimage(Long idimage) { this.idimage = idimage; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }
}