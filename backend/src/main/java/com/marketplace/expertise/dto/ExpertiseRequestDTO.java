package com.marketplace.expertise.dto;

import com.marketplace.expertise.entity.ExpertiseMethod;
import com.marketplace.expertise.entity.ExpertiseStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExpertiseRequestDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitImage;          // première image (fallback)
    private List<String> produitImages;   // ✅ liste complète des images (noms/fichiers)
    private String produitDescription;
    private String produitEtat;
    private String produitCategorie;
    private Double prixDebut;
    private Double prixFin;
    private String acheteurNom;
    private String datePublication;

    private Long vendeurId;
    private String vendeurNom;
    private Long expertId;
    private String expertFullName;
    private ExpertiseMethod method;
    private ExpertiseStatus status;
    private Double price;
    private Double expertShare;

    private List<String> slots; // ISO strings
    private String confirmedDateTime;
    private String location;
    private String expertResponseDeadline;
    private String reportSubmissionDeadline;

    // Informations additionnelles
    private String sellerName;
    private String expertiseLocation;
    private LocalDateTime appointmentDate;
}