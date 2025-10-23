package com.marketplace.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.controller.CategorieController;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.service.CategorieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategorieController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategorieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategorieService categorieService;

    private ObjectMapper objectMapper;
    private Categorie categorie1;
    private Categorie categorie2;
    private List<Categorie> categories;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        categorie1 = new Categorie();
        categorie1.setIdCategorie(1L);
        categorie1.setNomCategorie("Appareils");

        categorie2 = new Categorie();
        categorie2.setIdCategorie(2L);
        categorie2.setNomCategorie("Bijoux");

        categories = Arrays.asList(categorie1, categorie2);
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnCategoriesList_WhenDomaineExists() throws Exception {
        // Arrange
        Long domaineId = 1L;
        when(categorieService.getCategoriesByDomaine(domaineId)).thenReturn(categories);

        // Act & Assert
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idCategorie").value(1))
                .andExpect(jsonPath("$[0].nomCategorie").value("Appareils"))
                .andExpect(jsonPath("$[1].idCategorie").value(2))
                .andExpect(jsonPath("$[1].nomCategorie").value("Bijoux"));

        verify(categorieService).getCategoriesByDomaine(domaineId);
    }

    @Test
    void getCategoriesByDomaine_ShouldReturnEmptyList_WhenNoCategories() throws Exception {
        // Arrange
        Long domaineId = 99L;
        when(categorieService.getCategoriesByDomaine(domaineId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categorieService).getCategoriesByDomaine(domaineId);
    }

    @Test
    void getCategorieById_ShouldReturnCategorie_WhenExists() throws Exception {
        // Arrange
        Long categorieId = 1L;
        when(categorieService.getCategorieById(categorieId)).thenReturn(Optional.of(categorie1));

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categorieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCategorie").value(1))
                .andExpect(jsonPath("$.nomCategorie").value("Appareils"));

        verify(categorieService).getCategorieById(categorieId);
    }

    @Test
    void getCategorieById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Arrange
        Long categorieId = 99L;
        when(categorieService.getCategorieById(categorieId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categorieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categorieService).getCategorieById(categorieId);
    }

    @Test
    void getCategoriesByDomaine_ShouldHandlePathVariableCorrectly() throws Exception {
        // Arrange
        Long domaineId = 5L;
        when(categorieService.getCategoriesByDomaine(domaineId)).thenReturn(Arrays.asList(categorie1));

        // Act & Assert
        mockMvc.perform(get("/api/categories/domaine/{idDomaine}", domaineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idCategorie").value(1))
                .andExpect(jsonPath("$[0].nomCategorie").value("Appareils"));

        verify(categorieService).getCategoriesByDomaine(domaineId);
    }
}