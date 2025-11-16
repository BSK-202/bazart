package com.marketplace.user.entity;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.entity.Domaine;
import com.marketplace.user.entity.Client;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "expert")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String biography;

    @Column(name = "annees_experience")
    private int anneesExperience;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "domaine_id", nullable = false, referencedColumnName = "iddomaine")
    private Domaine domaine;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "expert_categories",
            joinColumns = @JoinColumn(name = "expert_id"),
            inverseJoinColumns = @JoinColumn(name = "categorie_id")
    )
    private List<Categorie> categories;

    @ElementCollection
    @CollectionTable(
            name = "expert_langues",
            joinColumns = @JoinColumn(name = "expert_id")
    )
    @Column(name = "langue")
    private List<String> langues;

    @Column(name = "is_active")
    private boolean isActive;

    // 🔥 MODIFICATION : Date d'embauche nullable
    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @ElementCollection
    @CollectionTable(
            name = "expert_signatures",
            joinColumns = @JoinColumn(name = "expert_id")
    )
    @Column(name = "signature_image")
    private List<String> signatureImages;

    @Column(name = "nombre_produits_expertise")
    private int nombreProduitsExpertise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Override
    public String toString() {
        return "Expert{" +
                "id=" + id +
                ", biography='" + biography + '\'' +
                ", anneesExperience=" + anneesExperience +
                ", domaine=" + (domaine != null ? domaine.getIdDomaine() : "null") +
                ", categories=" + (categories != null ? categories.size() : 0) +
                ", langues=" + langues +
                ", isActive=" + isActive +
                ", dateEmbauche=" + dateEmbauche +
                ", signatureImages=" + signatureImages +
                ", nombreProduitsExpertise=" + nombreProduitsExpertise +
                ", client=" + (client != null ? client.getId() : "null") +
                '}';
    }
}