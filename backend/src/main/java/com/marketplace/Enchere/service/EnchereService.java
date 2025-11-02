package com.marketplace.Enchere.service;


import com.marketplace.Enchere.dto.EnchereDTO;
import com.marketplace.Enchere.entity.Enchere;
import com.marketplace.Enchere.repository.EnchereRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EnchereService {

    @Autowired
    private EnchereRepository enchereRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProduitService produitService;

    // Placer une nouvelle enchère
    public EnchereDTO placerEnchere(Long produitId, Long clientId, Double montant) {
        // Vérifier que le produit existe et est en enchère
        Produit produit = produitService.getProduitById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        if (!"en_enchere".equals(produit.getEtat())) {
            throw new RuntimeException("Le produit n'est pas en enchère");
        }

        // Vérifier que l'enchère n'est pas terminée
        if (isEnchereTerminee(produit)) {
            throw new RuntimeException("L'enchère est terminée");
        }

        // Vérifier le client
        Client client = clientService.getClientById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Vérifier que le montant est supérieur à l'enchère actuelle
        Double montantActuel = getMontantActuel(produitId);
        if (montant <= montantActuel) {
            throw new RuntimeException("Le montant doit être supérieur à " + montantActuel);
        }

        // Vérifier que le montant est supérieur au prix de départ
        if (montant <= produit.getPrixDebut()) {
            throw new RuntimeException("Le montant doit être supérieur au prix de départ " + produit.getPrixDebut());
        }

        // Créer et sauvegarder l'enchère
        Enchere enchere = new Enchere(client, produit, montant);
        Enchere savedEnchere = enchereRepository.save(enchere);

        return convertToDTO(savedEnchere);
    }

    // Obtenir l'historique des enchères d'un produit
    public List<EnchereDTO> getHistoriqueEncheres(Long produitId) {
        List<Enchere> encheres = enchereRepository.findByProduitIdproduitOrderByMontantDesc(produitId);

        // Trouver le montant maximum pour marquer l'enchère en tête
        Double montantMax = encheres.stream()
                .map(Enchere::getMontant)
                .max(Double::compare)
                .orElse(0.0);

        return encheres.stream()
                .map(enchere -> {
                    EnchereDTO dto = convertToDTO(enchere);
                    dto.setIsLeading(enchere.getMontant().equals(montantMax));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Obtenir l'enchère actuelle (la plus haute) pour un produit
    public Optional<EnchereDTO> getEnchereActuelle(Long produitId) {
        return enchereRepository.findTopByProduitIdOrderByMontantDesc(produitId)
                .map(this::convertToDTO);
    }


    public Double getMontantActuel(Long produitId) {
        Optional<Double> maxMontant = enchereRepository.findMaxMontantByProduitId(produitId);

        if (maxMontant.isPresent()) {
            return maxMontant.get();
        } else {
            // Si pas d'enchère, retourner le prix de départ
            Produit produit = produitService.getProduitById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
            return produit.getPrixDebut();
        }
    }

    // Vérifier si l'enchère est terminée
    public boolean isEnchereTerminee(Produit produit) {
        if (produit.getDateEnchere() == null || produit.getDureeEnchereJours() == null) {
            return false;
        }

        LocalDateTime finEnchere = produit.getDateEnchere()
                .plusDays(produit.getDureeEnchereJours());

        return LocalDateTime.now().isAfter(finEnchere);
    }

    // Obtenir le temps restant pour une enchère
    public String getTempsRestant(Produit produit) {
        if (produit.getDateEnchere() == null || produit.getDureeEnchereJours() == null) {
            return "Enchère non démarrée";
        }

        LocalDateTime finEnchere = produit.getDateEnchere()
                .plusDays(produit.getDureeEnchereJours());
        LocalDateTime maintenant = LocalDateTime.now();

        if (maintenant.isAfter(finEnchere)) {
            return "Enchère terminée";
        }

        long seconds = java.time.Duration.between(maintenant, finEnchere).getSeconds();
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%dj %dh %dm %ds", days, hours, minutes, secs);
    }

    // Convertir Entity en DTO
    private EnchereDTO convertToDTO(Enchere enchere) {
        EnchereDTO dto = new EnchereDTO();
        dto.setIdEnchere(enchere.getIdEnchere());
        dto.setEncherisseurId(enchere.getEncherisseur().getIdclient());
        dto.setEncherisseurNom(enchere.getEncherisseur().getNom());
        dto.setEncherisseurPrenom(enchere.getEncherisseur().getPrenom());
        dto.setProduitId(enchere.getProduit().getIdproduit());
        dto.setProduitNom(enchere.getProduit().getNom());
        dto.setMontant(enchere.getMontant());
        dto.setDateEnchere(enchere.getDateEnchere());
        return dto;
    }

    // Obtenir les enchères d'un utilisateur
    public List<EnchereDTO> getEncheresByClient(Long clientId) {
        return enchereRepository.findByEncherisseurIdclient(clientId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}