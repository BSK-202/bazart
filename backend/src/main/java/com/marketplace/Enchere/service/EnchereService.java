package com.marketplace.Enchere.service;

import com.marketplace.Enchere.dto.EnchereDTO;
import com.marketplace.Enchere.entity.Enchere;
import com.marketplace.Enchere.repository.EnchereRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.notification.entity.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnchereService {

    @Autowired
    private EnchereRepository enchereRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProduitService produitService;

    @Autowired
    private NotificationService notificationService;

    // Placer une nouvelle enchère
    @Transactional
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

        // === ENVOYER LES NOTIFICATIONS ===
        envoyerNotificationsNouvelleEnchere(savedEnchere, produit, client);

        return convertToDTO(savedEnchere);
    }

    private void envoyerNotificationsNouvelleEnchere(Enchere nouvelleEnchere, Produit produit, Client nouveauEncherisseur) {
        Set<Long> recipients = new HashSet<>();

        // 1. Ajouter le vendeur du produit
        if (produit.getVendeur() != null) {
            recipients.add(produit.getVendeur().getIdclient());
            System.out.println("👤 Vendeur à notifier: " + produit.getVendeur().getIdclient());
        }

        // 2. Ajouter tous les clients ayant déjà participé à cette enchère (sauf le nouveau)
        List<Enchere> encheresPrecedentes = enchereRepository.findByProduitIdproduitOrderByMontantDesc(produit.getIdproduit());

        for (Enchere enchere : encheresPrecedentes) {
            if (enchere.getEncherisseur() != null &&
                    !enchere.getEncherisseur().getIdclient().equals(nouveauEncherisseur.getIdclient())) {
                recipients.add(enchere.getEncherisseur().getIdclient());
                System.out.println("👤 Ancien enchérisseur à notifier: " + enchere.getEncherisseur().getIdclient());
            }
        }

        // Exclure le nouvel enchérisseur lui-même
        recipients.remove(nouveauEncherisseur.getIdclient());

        if (!recipients.isEmpty()) {
            // Préparer les données de notification
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("productName", produit.getNom());
            notifData.put("bidAmount", nouvelleEnchere.getMontant());
            // Utiliser le nom et prénom réel de l'enchérisseur
            notifData.put("bidUserName", nouveauEncherisseur.getPrenom() + " " + nouveauEncherisseur.getNom());
            notifData.put("message", "Nouvelle enchère sur le produit \"" + produit.getNom() + "\"");
            notifData.put("productId", produit.getIdproduit());

            // Envoyer les notifications
            notificationService.processEvent(
                    NotificationType.NEW_BID,
                    recipients,
                    notifData
            );

            System.out.println("📢 Notifications envoyées à " + recipients.size() + " utilisateurs pour la nouvelle enchère");
            System.out.println("👤 Enchérisseur: " + nouveauEncherisseur.getPrenom() + " " + nouveauEncherisseur.getNom());
        } else {
            System.out.println("ℹ️ Aucun destinataire à notifier pour cette nouvelle enchère");
        }
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

    public Map<String, Object> getLeadingEncheresWithBlockedAmounts(Long clientId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> leadingEncheres = new ArrayList<>();

        try {
            // Vérifier que le client existe - UTILISER LA BONNE MÉTHODE
            Client client = clientService.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'ID: " + clientId));

            // Récupérer toutes les enchères de l'utilisateur
            List<Enchere> userEncheres = enchereRepository.findByEncherisseurIdclient(clientId);

            if (userEncheres.isEmpty()) {
                result.put("success", true);
                result.put("clientId", clientId);
                result.put("clientNom", client.getNom());
                result.put("clientPrenom", client.getPrenom());
                result.put("leadingEncheres", leadingEncheres);
                result.put("totalBlocked", 0.0);
                result.put("count", 0);
                result.put("message", "Aucune enchère trouvée");
                return result;
            }

            // Grouper par produit et trouver la plus haute enchère pour chaque produit
            Map<Long, Enchere> highestBidsByProduct = userEncheres.stream()
                    .collect(Collectors.toMap(
                            enchere -> enchere.getProduit().getIdproduit(),
                            enchere -> enchere,
                            (existing, replacement) ->
                                    existing.getMontant() > replacement.getMontant() ? existing : replacement
                    ));

            double totalBlocked = 0;

            // Pour chaque produit, vérifier si l'utilisateur est en tête
            for (Enchere userHighestBid : highestBidsByProduct.values()) {
                Long produitId = userHighestBid.getProduit().getIdproduit();
                Produit produit = userHighestBid.getProduit();

                // Trouver l'enchère la plus haute pour ce produit (tous utilisateurs)
                Optional<Enchere> topEnchere = enchereRepository.findTopByProduitIdOrderByMontantDesc(produitId);

                if (topEnchere.isPresent() &&
                        topEnchere.get().getEncherisseur().getIdclient().equals(clientId) &&
                        "en_enchere".equals(produit.getEtat())) {

                    // L'utilisateur est en tête de cette enchère
                    Map<String, Object> enchereInfo = new HashMap<>();
                    enchereInfo.put("produitId", produitId);
                    enchereInfo.put("produitNom", produit.getNom());
                    enchereInfo.put("blockedAmount", userHighestBid.getMontant());
                    enchereInfo.put("dateEnchere", userHighestBid.getDateEnchere());
                    enchereInfo.put("enchereId", userHighestBid.getIdEnchere());
                    enchereInfo.put("montantEnchere", userHighestBid.getMontant());

                    // Calculer le temps restant
                    enchereInfo.put("tempsRestant", getTempsRestant(produit));

                    leadingEncheres.add(enchereInfo);
                    totalBlocked += userHighestBid.getMontant();
                }
            }

            result.put("success", true);
            result.put("clientId", clientId);
            result.put("clientNom", client.getNom());
            result.put("clientPrenom", client.getPrenom());
            result.put("leadingEncheres", leadingEncheres);
            result.put("totalBlocked", totalBlocked);
            result.put("count", leadingEncheres.size());
            result.put("message", leadingEncheres.size() + " enchère(s) où vous êtes en tête");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }
}