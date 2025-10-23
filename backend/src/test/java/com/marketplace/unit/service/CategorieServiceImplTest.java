package com.marketplace.unit.service;

import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.repository.CategorieRepository;
import com.marketplace.catalog.service.CategorieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategorieServiceImplTest {

    @Mock
    private CategorieRepository categorieRepository;

    @InjectMocks
    private CategorieService categorieService;

    private Categorie categorie1;
    private Categorie categorie2;
    private List<Categorie> categories;

    @BeforeEach
    void setUp() {
        // Initialisation des données de test
        categorie1 = new Categorie();
        categorie1.setIdCategorie(1L);
        categorie1.setNomCategorie("Électronique");

        categorie2 = new Categorie();
        categorie2.setIdCategorie(2L);
        categorie2.setNomCategorie("Vêtements");

        categories = Arrays.asList(categorie1, categorie2);
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnCategories_WhenDomaineExists() {
        // Arrange
        Long domaineId = 1L;
        when(categorieRepository.findByDomaine_IdDomaine(domaineId)).thenReturn(categories);

        // Act
        List<Categorie> result = categorieService.getCategoriesByDomaine(domaineId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Électronique", result.get(0).getNomCategorie());
        assertEquals("Vêtements", result.get(1).getNomCategorie());

        verify(categorieRepository).findByDomaine_IdDomaine(domaineId);
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnEmptyList_WhenNoCategoriesFound() {
        // Arrange
        Long domaineId = 99L;
        when(categorieRepository.findByDomaine_IdDomaine(domaineId)).thenReturn(Arrays.asList());

        // Act
        List<Categorie> result = categorieService.getCategoriesByDomaine(domaineId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categorieRepository).findByDomaine_IdDomaine(domaineId);
    }

    @Test
    void getCategorieById_ShouldReturnCategorie_WhenCategorieExists() {
        // Arrange
        Long categorieId = 1L;
        when(categorieRepository.findById(categorieId)).thenReturn(Optional.of(categorie1));

        // Act
        Optional<Categorie> result = categorieService.getCategorieById(categorieId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(categorieId, result.get().getIdCategorie());
        assertEquals("Électronique", result.get().getNomCategorie());
        verify(categorieRepository).findById(categorieId);
    }

    @Test
    void getCategorieById_ShouldReturnEmpty_WhenCategorieDoesNotExist() {
        // Arrange
        Long categorieId = 99L;
        when(categorieRepository.findById(categorieId)).thenReturn(Optional.empty());

        // Act
        Optional<Categorie> result = categorieService.getCategorieById(categorieId);

        // Assert
        assertFalse(result.isPresent());
        verify(categorieRepository).findById(categorieId);
    }

    @Test
    void getCategorieById_ShouldReturnEmpty_WhenIdIsNull() {
        // Arrange
        when(categorieRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Optional<Categorie> result = categorieService.getCategorieById(null);

        // Assert
        assertFalse(result.isPresent());
        verify(categorieRepository).findById(null);
    }

    @Test
    void getCategoriesByDomaine_ShouldVerifyRepositoryCall() {
        // Arrange
        Long domaineId = 5L;
        when(categorieRepository.findByDomaine_IdDomaine(domaineId)).thenReturn(Arrays.asList(categorie1));

        // Act
        List<Categorie> result = categorieService.getCategoriesByDomaine(domaineId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Électronique", result.get(0).getNomCategorie());
        verify(categorieRepository).findByDomaine_IdDomaine(domaineId);
    }
}