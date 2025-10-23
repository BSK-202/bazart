package com.marketplace.unit.controller;

import com.marketplace.catalog.controller.CategorieController;
import com.marketplace.catalog.service.CategorieService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategorieController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategorieImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategorieService categorieService;

    // Tests avec la VRAIE image appareils.jpg qui existe dans assets/categories/

    @Test
    void getCategorieImage_ShouldReturnImage_WhenRealImageExists() throws Exception {
        // Arrange - Utiliser l'image qui existe VRAIMENT
        String fileName = "appareils.jpg";

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("Content-Type", "image/jpeg")) // ou autre type
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    @Test
    void getCategorieImage_ShouldSetCorrectCorsHeaders_WhenRealImage() throws Exception {
        // Arrange
        String fileName = "appareils.jpg";

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET, OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "*"));
    }

    @Test
    void getCategorieImage_ShouldSetNoCacheHeaders_WhenRealImage() throws Exception {
        // Arrange
        String fileName = "appareils.jpg";

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    @Test
    void getCategorieImage_ShouldReturnCorrectContentDisposition_WhenRealImage() throws Exception {
        // Arrange
        String fileName = "appareils.jpg";

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"appareils.jpg\""));
    }

    // Tests avec fichiers qui n'existent PAS
    @Test
    void getCategorieImage_ShouldReturnNotFound_WhenFileDoesNotExist() throws Exception {
        // Arrange
        String fileName = "non-existent-image-12345.jpg";

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategorieImage_ShouldReturnNotFound_WhenFileIsNull() throws Exception {
        // Arrange
        String fileName = ""; // ou tester avec null si votre API le permet

        // Act & Assert
        mockMvc.perform(get("/api/categories/images/{fileName}", fileName))
                .andExpect(status().isNotFound());
    }
}