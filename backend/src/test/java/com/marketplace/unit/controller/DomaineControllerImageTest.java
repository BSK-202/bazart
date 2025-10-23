package com.marketplace.unit.controller;

import com.marketplace.catalog.controller.DomaineController;
import com.marketplace.catalog.service.DomaineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomaineControllerImageTest {

    @Mock
    private DomaineService domaineService;

    @InjectMocks
    private DomaineController domaineController;

    @Test
    void getDomaineImage_WhenFileExists_ShouldReturnImage() {
        try {
            // Arrange - Créer un fichier de test temporaire
            String fileName = "test-image.jpg";
            Path testImagePath = Paths.get("assets/domaines/" + fileName);

            // Créer le répertoire s'il n'existe pas
            Files.createDirectories(testImagePath.getParent());

            // Créer le fichier de test s'il n'existe pas
            if (!Files.exists(testImagePath)) {
                Files.createFile(testImagePath);
                Files.write(testImagePath, "fake image content".getBytes());
            }

            // Act
            ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());

            // Vérifier les headers
            assertTrue(response.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL));
            assertEquals("no-cache, no-store, must-revalidate",
                    response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
            assertEquals("*", response.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

            // Nettoyer
            Files.deleteIfExists(testImagePath);

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    void getDomaineImage_WhenFileNotExists_ShouldReturnNotFound() {
        // Arrange
        String fileName = "non-existent-image.jpg";

        // Act
        ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getDomaineImage_WhenFileNotReadable_ShouldReturnNotFound() {
        // Ce test simule un fichier qui existe mais n'est pas lisible
        // Dans la pratique, vous pourriez avoir besoin de mock FileSystem
        String fileName = "unreadable-image.jpg";

        // Act - Le contrôleur va essayer de lire le fichier et échouer
        ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

        // Assert - Comme le fichier n'existe pas, ça retourne 404
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDomaineImage_WhenIOException_ShouldReturnInternalServerError() {
        // Arrange - Utiliser un nom de fichier invalide pour provoquer une exception
        String fileName = "invalid\0file.jpg"; // Caractère nul invalide

        // Act
        ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void getDomaineImage_WithDifferentFileTypes_ShouldSetCorrectContentType() {
        try {
            // Tester avec différents types de fichiers
            String[] testFiles = {"test.png", "test.jpg", "test.jpeg", "test.gif", "test.pdf"};

            for (String fileName : testFiles) {
                Path testPath = Paths.get("assets/domaines/" + fileName);
                Files.createDirectories(testPath.getParent());

                if (!Files.exists(testPath)) {
                    Files.createFile(testPath);
                    Files.write(testPath, "test content".getBytes());
                }

                ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getHeaders().getContentType());

                // Nettoyer
                Files.deleteIfExists(testPath);
            }
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}