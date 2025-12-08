package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.catalog.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProduitServiceImpl implements ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitServiceImpl(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @Override
    public Optional<Produit> getProduitById(Long id) {
        return produitRepository.findById(id);
    }

    @Override
    public List<Produit> getProduitsAcceptesByCategorie(Long idCategorie) {
        List<Produit> produits = new ArrayList<>();
        produits.addAll(produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "accepte"));
        produits.addAll(produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "expertise_validee"));
        produits.addAll(produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "en_enchere"));
        produits.addAll(produitRepository.findByCategorieIdCategorieAndEtat(idCategorie, "expertise_refusee"));

        // dédoublonnage
        produits = produits.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Produit::getIdproduit, p -> p, (p1, p2) -> p1),
                        m -> new ArrayList<>(m.values())
                ));
        System.out.println("📦 Produits acceptés trouvés: " + produits.size());
        return produits;
    }

    @Override
    public List<Produit> getProduitsByCategorie(Long idCategorie) {
        return produitRepository.findByCategorieIdCategorie(idCategorie);
    }

    @Override
    public List<String> getTousLesEtats() {
        return produitRepository.findDistinctEtats(); //  CORRIGÉ : findDistinctEtats()
    }

    @Override
    public Produit saveProduit(Produit produit) {
        return produitRepository.save(produit);
    }

    @Override
    public List<Produit> getProduitsEnAttente() {
        return produitRepository.findByEtat("en_attente");
    }

    @Override
    public long countProduitsEnAttente() {
        return produitRepository.countByEtat("en_attente");
    }
    @Override
    public List<Produit> getProduitsByVendeur(Long vendeurId) {
        return produitRepository.findByVendeurIdclient(vendeurId);
    }
    @Override
    public void deleteProduitImages(Produit produit, List<String> imageUrlsToDelete) {
        if (imageUrlsToDelete == null || imageUrlsToDelete.isEmpty()) {
            return;
        }

        System.out.println("🗑️ Suppression de " + imageUrlsToDelete.size() + " images depuis le service");

        produit.getImages().removeIf(image -> {
            boolean shouldRemove = imageUrlsToDelete.contains(image.getUrl());
            if (shouldRemove) {
                System.out.println("✅ Image supprimée de la base: " + image.getUrl());
            }
            return shouldRemove;
        });

        System.out.println("📊 Images restantes après suppression: " + produit.getImages().size());
    }

    @Override
    public void addProduitImages(Produit produit, List<MultipartFile> newImages, Path produitFolderPath) throws IOException {
        if (newImages == null || newImages.isEmpty()) {
            return;
        }

        System.out.println("🖼️ Ajout de " + newImages.size() + " nouvelles images depuis le service");

        // Initialiser la liste si null
        if (produit.getImages() == null) {
            produit.setImages(new ArrayList<>());
        }

        int nextIndex = findNextAvailableImageIndex(produit.getImages());

        for (int i = 0; i < newImages.size(); i++) {
            MultipartFile file = newImages.get(i);
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String fileName = "image_" + (nextIndex + i) + fileExtension;

            Path imagePath = produitFolderPath.resolve(fileName);

            Files.write(imagePath, file.getBytes());
            System.out.println("✅ Nouvelle image sauvegardée: " + fileName);

            ProduitImage produitImage = new ProduitImage();
            produitImage.setUrl(fileName);
            produitImage.setProduit(produit);
            produit.getImages().add(produitImage);
        }
    }

    // MÉTHODES UTILITAIRES
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private boolean isImageAlreadyExists(List<ProduitImage> existingImages, String fileName) {
        return existingImages.stream()
                .anyMatch(img -> fileName.equals(img.getUrl()));
    }

    private int findNextAvailableImageIndex(List<ProduitImage> existingImages) {
        if (existingImages == null || existingImages.isEmpty()) {
            return 1;
        }

        Set<Integer> existingNumbers = new HashSet<>();
        for (ProduitImage image : existingImages) {
            String url = image.getUrl();
            if (url != null && url.startsWith("image_")) {
                try {
                    String numberStr = url.substring(6, url.lastIndexOf('.'));
                    int number = Integer.parseInt(numberStr);
                    existingNumbers.add(number);
                } catch (Exception e) {
                    System.err.println("⚠️ Impossible d'extraire le numéro de l'image: " + url);
                }
            }
        }

        int nextNumber = 1;
        while (existingNumbers.contains(nextNumber)) {
            nextNumber++;
        }

        System.out.println("🔢 Prochain index d'image disponible: " + nextNumber);
        return nextNumber;
    }
    // 🆕 MÉTHODE POUR RÉCUPÉRER LES PRODUITS PAR ÉTAT
    public List<Produit> getProduitsByEtat(String etat) {
        try {
            System.out.println("🔍 Service: Recherche des produits avec état: " + etat);
            List<Produit> produits = produitRepository.findByEtat(etat);
            System.out.println("✅ Service: " + produits.size() + " produits trouvés avec état: " + etat);
            return produits;
        } catch (Exception e) {
            System.err.println("❌ Service: Erreur lors de la recherche des produits par état: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des produits par état", e);
        }
    }

    // Dans ProduitServiceImpl.java, ajouter cette implémentation
    @Override
    public List<Produit> getProduitsByAcheteur(Long acheteurId) {
        try {
            System.out.println("🔍 Service: Recherche des produits pour l'acheteur ID: " + acheteurId);
            List<Produit> produits = produitRepository.findByAcheteurIdclient(acheteurId);
            System.out.println("✅ Service: " + produits.size() + " produits trouvés pour l'acheteur");
            return produits;
        } catch (Exception e) {
            System.err.println("❌ Service: Erreur lors de la recherche des produits par acheteur: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des produits par acheteur", e);
        }
    }

}
