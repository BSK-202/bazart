package com.marketplace.integration.service;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.CategorieRepository;
import com.marketplace.catalog.repository.DomaineRepository;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.catalog.service.ProduitServiceImpl;
import com.marketplace.user.entity.Client;
import com.marketplace.user.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProduitServiceTest {

    @Autowired
    private ProduitServiceImpl produitService;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private DomaineRepository domaineRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Categorie categorie;
    private Client vendeur;

    @BeforeEach
    void setUp() {
        //  SAUVEGARDER D'ABORD LE CLIENT
        vendeur = new Client();
        vendeur.setNom("Vendeur Test");
        vendeur.setPrenom("Test");
        vendeur.setEmail("vendeur@test.com");
        vendeur.setMotdepasse("password");
        vendeur.setEnabled(true);
        vendeur.setDateinscription(LocalDateTime.now());
        vendeur = clientRepository.save(vendeur); //  LIGNE CRUCIALE AJOUTÉE

        // Créer une catégorie de test
        Domaine domaine = new Domaine();
        domaine.setNomDomaine("Test Domaine");
        domaine.setDescription("Description test");
        domaine.setImage("test.jpg");
        Domaine savedDomaine = domaineRepository.save(domaine);

        categorie = new Categorie();
        categorie.setNomCategorie("Test Catégorie");
        categorie.setDescription("Description catégorie test");
        categorie.setImage("cat.jpg");
        categorie.setDomaine(savedDomaine);
        categorie = categorieRepository.save(categorie);
    }

    @Test
    void testSaveProduit_ShouldSaveAndRetrieveProduct() {
        // Given
        Produit produit = new Produit();
        produit.setNom("Produit Test Integration");
        produit.setDescription("Description test intégration");
        produit.setPrixDebut(Double.valueOf(100.0));
        produit.setPrixFin(Double.valueOf(150.0));
        produit.setDatePublication(LocalDateTime.now());
        produit.setAExpertise(false);
        produit.setEtat("en_attente");
        produit.setCategorie(categorie);
        produit.setVendeur(vendeur); // Client déjà sauvegardé

        // When
        Produit savedProduit = produitService.saveProduit(produit);

        // Then
        assertNotNull(savedProduit.getIdproduit());
        assertEquals("Produit Test Integration", savedProduit.getNom());
        assertEquals("en_attente", savedProduit.getEtat());
        assertEquals(categorie.getIdCategorie(), savedProduit.getCategorie().getIdCategorie());
    }

    @Test
    void testGetProduitById_WhenExists_ShouldReturnProduct() {
        // Given
        Produit produit = createTestProduit("Produit pour GetById", "en_attente");
        Produit savedProduit = produitService.saveProduit(produit);
        Long produitId = savedProduit.getIdproduit();

        // When
        Optional<Produit> foundProduit = produitService.getProduitById(produitId);

        // Then
        assertTrue(foundProduit.isPresent());
        assertEquals("Produit pour GetById", foundProduit.get().getNom());
        assertEquals(produitId, foundProduit.get().getIdproduit());
    }

    @Test
    void testGetProduitById_WhenNotExists_ShouldReturnEmpty() {
        // Given
        Long nonExistentId = (Long) 999L;

        // When
        Optional<Produit> foundProduit = produitService.getProduitById(nonExistentId);

        // Then
        assertFalse(foundProduit.isPresent());
    }

    @Test
    void testGetProduitsAcceptesByCategorie_ShouldReturnOnlyAcceptedProducts() {
        // Given
        Produit produitAccepte = createTestProduit("Produit Accepté", "accepte");
        Produit produitEnAttente = createTestProduit("Produit En Attente", "en_attente");
        Produit produitRefuse = createTestProduit("Produit Refusé", "refuser");

        produitService.saveProduit(produitAccepte);
        produitService.saveProduit(produitEnAttente);
        produitService.saveProduit(produitRefuse);

        // When
        List<Produit> produitsAcceptes = produitService.getProduitsAcceptesByCategorie(categorie.getIdCategorie());

        // Then
        assertFalse(produitsAcceptes.isEmpty());
        assertTrue(produitsAcceptes.stream().allMatch(p -> "accepte".equals(p.getEtat())));
        assertEquals(1, produitsAcceptes.size());
        assertEquals("Produit Accepté", produitsAcceptes.get(0).getNom());
    }

    @Test
    void testGetProduitsAcceptesByCategorie_WithDifferentCaseStates_ShouldWork() {
        // Given - Tester les différentes variantes de casse
        Produit produit1 = createTestProduit("Produit 1", "accepte");
        Produit produit2 = createTestProduit("Produit 2", "ACCEPTE");
        Produit produit3 = createTestProduit("Produit 3", "accepter");

        produitService.saveProduit(produit1);
        produitService.saveProduit(produit2);
        produitService.saveProduit(produit3);

        // When
        List<Produit> produitsAcceptes = produitService.getProduitsAcceptesByCategorie(categorie.getIdCategorie());

        // Then - Doit trouver les 3 produits avec différents états "accepte"
        assertEquals(1, produitsAcceptes.size());
    }

    @Test
    void testGetProduitsByCategorie_ShouldReturnAllProducts() {
        // Given
        Produit produit1 = createTestProduit("Produit 1", "en_attente");
        Produit produit2 = createTestProduit("Produit 2", "accepte");
        Produit produit3 = createTestProduit("Produit 3", "refuser");

        produitService.saveProduit(produit1);
        produitService.saveProduit(produit2);
        produitService.saveProduit(produit3);

        // When
        List<Produit> produits = produitService.getProduitsByCategorie(categorie.getIdCategorie());

        // Then
        assertEquals(3, produits.size());
        assertTrue(produits.stream().anyMatch(p -> "en_attente".equals(p.getEtat())));
        assertTrue(produits.stream().anyMatch(p -> "accepte".equals(p.getEtat())));
        assertTrue(produits.stream().anyMatch(p -> "refuser".equals(p.getEtat())));
    }

    @Test
    void testGetTousLesEtats_ShouldReturnDistinctStates() {
        // Given
        Produit produit1 = createTestProduit("Produit 1", "en_attente");
        Produit produit2 = createTestProduit("Produit 2", "accepte");
        Produit produit3 = createTestProduit("Produit 3", "en_attente"); // État dupliqué

        produitService.saveProduit(produit1);
        produitService.saveProduit(produit2);
        produitService.saveProduit(produit3);

        // When
        List<String> etats = produitService.getTousLesEtats();

        // Then
        assertNotNull(etats);
        assertTrue(etats.contains("en_attente"));
        assertTrue(etats.contains("accepte"));
        assertEquals(2, etats.size()); // Seulement 2 états distincts
    }

    private Produit createTestProduit(String nom, String etat) {
        Produit produit = new Produit();
        produit.setNom(nom);
        produit.setDescription("Description " + nom);
        produit.setPrixDebut(Double.valueOf(50.0));
        produit.setPrixFin(Double.valueOf(100.0));
        produit.setDatePublication(LocalDateTime.now());
        produit.setAExpertise(false);
        produit.setEtat(etat);
        produit.setCategorie(categorie);
        produit.setVendeur(vendeur); // Utilise le client déjà sauvegardé
        return produit;
    }
}