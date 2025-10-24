// CommentaireService.java - VERSION CORRIGÉE
package com.marketplace.interaction.service;

import com.marketplace.interaction.entity.Commentaire;
import com.marketplace.interaction.repository.CommentaireRepository;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentaireService {

    @Autowired
    private CommentaireRepository commentaireRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private ClientRepository clientRepository;

    // ✅ CORRECTION : Utiliser une requête JOIN FETCH pour charger les relations
    public List<Commentaire> getCommentairesByProduit(Long produitId) {
        return commentaireRepository.findByProduitIdWithClient(produitId);
    }

    // CommentaireService.java - CORRECTION
    public Commentaire addCommentaire(Long produitId, Long clientId, String contenu) {
        Optional<Produit> produitOpt = produitRepository.findById(produitId);
        Optional<Client> clientOpt = clientRepository.findById(clientId);

        if (produitOpt.isPresent() && clientOpt.isPresent()) {
            Commentaire commentaire = new Commentaire();
            commentaire.setContenu(contenu);
            commentaire.setDate(LocalDateTime.now());
            commentaire.setProduit(produitOpt.get());
            commentaire.setClient(clientOpt.get());

            Commentaire savedCommentaire = commentaireRepository.save(commentaire);

            // ✅ FORCER le chargement du client
            savedCommentaire = commentaireRepository.findByIdWithClient(savedCommentaire.getIdcommentaire())
                    .orElseThrow(() -> new RuntimeException("Erreur lors de la création du commentaire"));

            return savedCommentaire;
        }
        throw new RuntimeException("Produit ou Client non trouvé");
    }

    public void deleteCommentaire(Long commentaireId, Long clientId) {
        Optional<Commentaire> commentaireOpt = commentaireRepository.findById(commentaireId);
        if (commentaireOpt.isPresent()) {
            Commentaire commentaire = commentaireOpt.get();
            // ✅ Vérifier si l'utilisateur est le propriétaire du commentaire
            if (commentaire.getClient().getIdclient().equals(clientId)) {
                commentaireRepository.deleteById(commentaireId);
            } else {
                throw new RuntimeException("Non autorisé à supprimer ce commentaire");
            }
        } else {
            throw new RuntimeException("Commentaire non trouvé");
        }
    }

    public int getNombreCommentaires(Long produitId) {
        return commentaireRepository.countByProduitId(produitId);
    }
}