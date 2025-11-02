package com.marketplace.interaction.service;

import com.marketplace.interaction.entity.Interaction;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.user.entity.Client;
import com.marketplace.interaction.repository.InteractionRepository;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InteractionService {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean toggleInteraction(Long produitId, Long clientId) {
        System.out.println("🔵 toggleInteraction - produitId: " + produitId + ", clientId: " + clientId);

        try {
            // Vérifier si l'interaction existe déjà
            Optional<Interaction> existingInteraction = interactionRepository.findByProduitIdAndClientId(produitId, clientId);

            if (existingInteraction.isPresent()) {
                // Si existe, supprimer (unlike)
                System.out.println("🟡 Suppression du like existant");
                interactionRepository.delete(existingInteraction.get());
                return false;
            } else {
                // Si n'existe pas, créer (like)
                System.out.println("🟢 Création d'un nouveau like");
                Interaction interaction = new Interaction();
                interaction.setDate(LocalDateTime.now());

                Produit produit = produitRepository.findById(produitId)
                        .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID: " + produitId));
                interaction.setProduit(produit);

                Client client = clientRepository.findById(clientId)
                        .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'ID: " + clientId));
                interaction.setClient(client);

                interactionRepository.save(interaction);
                return true;
            }
        } catch (Exception e) {
            System.err.println("🔴 Erreur dans toggleInteraction: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean hasClientLikedProduct(Long produitId, Long clientId) {
        return interactionRepository.findByProduitIdAndClientId(produitId, clientId).isPresent();
    }

    public int getInteractionCount(Long produitId) {
        return interactionRepository.countByProduitId(produitId);
    }
    // Dans InteractionService.java
    public List<Interaction> getInteractionsByClientId(Long clientId) {
        return interactionRepository.findByClientId(clientId);
    }


}