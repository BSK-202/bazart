package com.marketplace.catalog.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.marketplace.expertise.entity.ExpertiseMethod;
import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.entity.Interaction;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
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

    public String getEtat_expertise() {
        return etat_expertise;
    }

    public void setEtat_expertise(String etat_expertise) {
        this.etat_expertise = etat_expertise;
    }

    @Column(nullable = true)
    private String etat_expertise; // null, "en_attente", "accepter", "refuser"

    @ManyToOne
    @JoinColumn(name = "idclient", referencedColumnName = "idclient")
    private Client vendeur;

    @ManyToOne
    @JoinColumn(name = "idclientacheteur", referencedColumnName = "idclient")
    private Client acheteur;

    @ManyToOne
    @JoinColumn(name = "idcategorie", referencedColumnName = "idcategorie")
    private Categorie categorie;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProduitImage> images=new ArrayList<>();;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Interaction> interactions;

    @OneToMany(mappedBy = "produit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Commentaire> commentaires;

    @Column(name = "duree_enchere_jours")
    private Integer dureeEnchereJours;

    //----------------------------------Attribut et methodes d'expertisation--------------------------------------------------------------
    @Column(name = "expertise_method")
    @Enumerated(EnumType.STRING)
    private ExpertiseMethod expertiseMethod; // ONLINE / ONSITE

    @Column(name = "expertise_request_id")
    private Long expertiseRequestId; // id de la demande courante (facultatif mais pratique)

    // 🆕 3 créneaux d'expertise proposés par le vendeur
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "expertise_slot1")
    private LocalDateTime expertiseSlot1;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "expertise_slot2")
    private LocalDateTime expertiseSlot2;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "expertise_slot3")
    private LocalDateTime expertiseSlot3;

    public ExpertiseMethod getExpertiseMethod() {
        return expertiseMethod;
    }

    public void setExpertiseMethod(ExpertiseMethod expertiseMethod) {
        this.expertiseMethod = expertiseMethod;
    }

    public Long getExpertiseRequestId() {
        return expertiseRequestId;
    }
    public void setExpertiseRequestId(Long expertiseRequestID) { this.expertiseRequestId = expertiseRequestID; }

    public LocalDateTime getExpertiseSlot1() {
        return expertiseSlot1;
    }

    public void setExpertiseSlot1(LocalDateTime expertiseSlot1) {
        this.expertiseSlot1 = expertiseSlot1;
    }

    public LocalDateTime getExpertiseSlot2() {
        return expertiseSlot2;
    }

    public void setExpertiseSlot2(LocalDateTime expertiseSlot2) {
        this.expertiseSlot2 = expertiseSlot2;
    }

    public LocalDateTime getExpertiseSlot3() {
        return expertiseSlot3;
    }

    public void setExpertiseSlot3(LocalDateTime expertiseSlot3) {
        this.expertiseSlot3 = expertiseSlot3;
    }

    //----------------------------------------------------------------------------------------------------------------------------------------
    public Integer getDureeEnchereJours() {
        return dureeEnchereJours;
    }

    public void setDureeEnchereJours(Integer dureeEnchereJours) {
        this.dureeEnchereJours = dureeEnchereJours;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------

    //Expertisation

    // Public info to display on product detail
    @Column(name = "expertise_public_comment", columnDefinition = "TEXT")
    private String expertisePublicComment;

    @Column(name = "expertise_authenticity_level")
    private String expertiseAuthenticityLevel;

    @Column(name = "expertise_product_condition")
    private String expertiseProductCondition;

    @Column(name = "expertise_approved")
    private Boolean expertiseApproved = false;

    public Boolean getExpertiseApproved() {
        return expertiseApproved != null ? expertiseApproved : false;
    }

    public void setExpertiseApproved(Boolean expertiseApproved) {
        this.expertiseApproved = expertiseApproved != null ? expertiseApproved : false;
    }


    // getters/setters…
    public String getExpertisePublicComment() { return expertisePublicComment; }
    public void setExpertisePublicComment(String expertisePublicComment) { this.expertisePublicComment = expertisePublicComment; }

    public String getExpertiseAuthenticityLevel() { return expertiseAuthenticityLevel; }
    public void setExpertiseAuthenticityLevel(String expertiseAuthenticityLevel) { this.expertiseAuthenticityLevel = expertiseAuthenticityLevel; }

    public String getExpertiseProductCondition() { return expertiseProductCondition; }
    public void setExpertiseProductCondition(String expertiseProductCondition) { this.expertiseProductCondition = expertiseProductCondition; }

    public boolean isExpertiseApproved() { return expertiseApproved; }
    public void setExpertiseApproved(boolean expertiseApproved) { this.expertiseApproved = expertiseApproved; }


    //---------------------------------------------------------------------------------------------------------------------------------------------




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
    public void setImages(List<ProduitImage> images) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.clear(); // On vide la collection existante
        if (images != null) {
            for (ProduitImage image : images) {
                image.setProduit(this); // IMPORTANT: établir la relation bidirectionnelle
                this.images.add(image);
            }
        }
    }


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

    public void setIdClientAcheteur(Long idGagnant) {
        if (idGagnant != null) {
            Client client = new Client();
            client.setIdclient(idGagnant);
            this.acheteur = client;
        } else {
            this.acheteur = null;
        }
    }


}
