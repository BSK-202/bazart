package com.marketplace.unit.controller;

import com.marketplace.catalog.controller.ProduitController;
import com.marketplace.catalog.service.CategorieService;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.user.service.ClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitControllerImageTest {

    @Mock
    private ProduitService produitService;

    @Mock
    private CategorieService categorieService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ProduitController produitController;

    @Test
    void getImage_WhenFileNotExists_ShouldReturnNotFound() {
        // Act - Le fichier n'existe pas dans l'environnement de test
        ResponseEntity<Resource> response = produitController.getImage(1L, "non-existent.jpg");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getImage_WhenIOException_ShouldReturnInternalServerError() {
        // Arrange - Utiliser un nom de fichier invalide
        String invalidFileName = "invalid\0file.jpg";

        // Act
        ResponseEntity<Resource> response = produitController.getImage(1L, invalidFileName);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}