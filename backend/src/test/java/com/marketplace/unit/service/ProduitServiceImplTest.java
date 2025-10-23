package com.marketplace.unit.service;

import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.catalog.service.ProduitServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceImplTest {

    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private ProduitServiceImpl produitService;

    @Test
    void getProduitById_WhenProductExists_ShouldReturnProduct() {
        // Arrange
        Long productId = 1L;
        Produit expectedProduct = new Produit();
        expectedProduct.setIdproduit(productId);
        expectedProduct.setNom("Laptop");

        when(produitRepository.findById(productId)).thenReturn(Optional.of(expectedProduct));

        // Act
        Optional<Produit> result = produitService.getProduitById(productId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(productId, result.get().getIdproduit());
        assertEquals("Laptop", result.get().getNom());
        verify(produitRepository, times(1)).findById(productId);
    }

    @Test
    void getProduitById_WhenProductNotExists_ShouldReturnEmpty() {
        // Arrange
        Long productId = 999L;
        when(produitRepository.findById(productId)).thenReturn(Optional.empty());

        // Act
        Optional<Produit> result = produitService.getProduitById(productId);

        // Assert
        assertTrue(result.isEmpty());
        verify(produitRepository, times(1)).findById(productId);
    }

    @Test
    void getProduitsAcceptesByCategorie_WithLowerCaseEtat_ShouldReturnProducts() {
        // Arrange
        Long categorieId = 1L;
        Produit produit1 = new Produit();
        produit1.setIdproduit(1L);
        produit1.setNom("Produit 1");
        produit1.setEtat("accepte");

        Produit produit2 = new Produit();
        produit2.setIdproduit(2L);
        produit2.setNom("Produit 2");
        produit2.setEtat("accepte");

        List<Produit> expectedProducts = Arrays.asList(produit1, produit2);

        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepte"))
                .thenReturn(expectedProducts);

        // Act
        List<Produit> result = produitService.getProduitsAcceptesByCategorie(categorieId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("accepte", result.get(0).getEtat());
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepte");
        verify(produitRepository, never()).findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE");
        verify(produitRepository, never()).findByCategorieIdCategorieAndEtat(categorieId, "accepter");
    }

    @Test
    void getProduitsAcceptesByCategorie_WithUpperCaseEtat_ShouldReturnProducts() {
        // Arrange
        Long categorieId = 1L;
        Produit produit = new Produit();
        produit.setIdproduit(1L);
        produit.setNom("Produit 1");
        produit.setEtat("ACCEPTE");

        List<Produit> emptyList = List.of();
        List<Produit> expectedProducts = List.of(produit);

        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepte"))
                .thenReturn(emptyList);
        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE"))
                .thenReturn(expectedProducts);

        // Act
        List<Produit> result = produitService.getProduitsAcceptesByCategorie(categorieId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACCEPTE", result.get(0).getEtat());
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepte");
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE");
        verify(produitRepository, never()).findByCategorieIdCategorieAndEtat(categorieId, "accepter");
    }

    @Test
    void getProduitsAcceptesByCategorie_WithAccepterEtat_ShouldReturnProducts() {
        // Arrange
        Long categorieId = 1L;
        Produit produit = new Produit();
        produit.setIdproduit(1L);
        produit.setNom("Produit 1");
        produit.setEtat("accepter");

        List<Produit> emptyList = List.of();
        List<Produit> expectedProducts = List.of(produit);

        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepte"))
                .thenReturn(emptyList);
        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE"))
                .thenReturn(emptyList);
        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepter"))
                .thenReturn(expectedProducts);

        // Act
        List<Produit> result = produitService.getProduitsAcceptesByCategorie(categorieId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("accepter", result.get(0).getEtat());
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepte");
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE");
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepter");
    }

    @Test
    void getProduitsAcceptesByCategorie_WhenNoProducts_ShouldReturnEmptyList() {
        // Arrange
        Long categorieId = 1L;
        List<Produit> emptyList = List.of();

        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepte"))
                .thenReturn(emptyList);
        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE"))
                .thenReturn(emptyList);
        when(produitRepository.findByCategorieIdCategorieAndEtat(categorieId, "accepter"))
                .thenReturn(emptyList);

        // Act
        List<Produit> result = produitService.getProduitsAcceptesByCategorie(categorieId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepte");
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "ACCEPTE");
        verify(produitRepository, times(1)).findByCategorieIdCategorieAndEtat(categorieId, "accepter");
    }

    @Test
    void getProduitsByCategorie_ShouldReturnProducts() {
        // Arrange
        Long categorieId = 1L;
        Produit produit1 = new Produit();
        produit1.setIdproduit(1L);
        produit1.setNom("Produit 1");

        Produit produit2 = new Produit();
        produit2.setIdproduit(2L);
        produit2.setNom("Produit 2");

        List<Produit> expectedProducts = Arrays.asList(produit1, produit2);

        when(produitRepository.findByCategorieIdCategorie(categorieId))
                .thenReturn(expectedProducts);

        // Act
        List<Produit> result = produitService.getProduitsByCategorie(categorieId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(produitRepository, times(1)).findByCategorieIdCategorie(categorieId);
    }

    @Test
    void getTousLesEtats_ShouldReturnDistinctEtats() {
        // Arrange
        List<String> expectedEtats = Arrays.asList("accepte", "en attente", "refuse");

        when(produitRepository.findDistinctEtats()).thenReturn(expectedEtats);

        // Act
        List<String> result = produitService.getTousLesEtats();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("accepte"));
        assertTrue(result.contains("en attente"));
        assertTrue(result.contains("refuse"));
        verify(produitRepository, times(1)).findDistinctEtats();
    }

    @Test
    void saveProduit_ShouldSaveAndReturnProduct() {
        // Arrange
        Produit produitToSave = new Produit();
        produitToSave.setNom("Nouveau Produit");
        produitToSave.setPrixDebut(99.99);

        Produit savedProduit = new Produit();
        savedProduit.setIdproduit(1L);
        savedProduit.setNom("Nouveau Produit");
        savedProduit.setPrixDebut(99.99);

        when(produitRepository.save(produitToSave)).thenReturn(savedProduit);

        // Act
        Produit result = produitService.saveProduit(produitToSave);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getIdproduit());
        assertEquals("Nouveau Produit", result.getNom());
        assertEquals(99.99, result.getPrixDebut());
        verify(produitRepository, times(1)).save(produitToSave);
    }

    @Test
    void saveProduit_WithNull_ShouldHandleGracefully() {
        // Arrange
        when(produitRepository.save(null)).thenThrow(new IllegalArgumentException());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            produitService.saveProduit(null);
        });

        verify(produitRepository, times(1)).save(null);
    }
}