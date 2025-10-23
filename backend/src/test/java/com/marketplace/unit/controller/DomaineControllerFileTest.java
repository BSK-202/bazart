package com.marketplace.unit.controller;

import com.marketplace.catalog.controller.DomaineController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DomaineControllerFileTest {

    private DomaineController domaineController;

    @TempDir
    Path tempDir; // Répertoire temporaire pour les tests

    @BeforeEach
    void setUp() {
        domaineController = new DomaineController(null);

        // Utiliser Reflection pour changer UPLOAD_DIR pour les tests
        try {
            var field = DomaineController.class.getDeclaredField("UPLOAD_DIR");
            field.setAccessible(true);
            field.set(domaineController, tempDir.toString() + "/");
        } catch (Exception e) {
            fail("Failed to set UPLOAD_DIR for testing: " + e.getMessage());
        }
    }

    @Test
    void getDomaineImage_WithTempFile_ShouldWork() throws Exception {
        // Arrange
        String fileName = "accessoires.jpg";
        Path testFile = tempDir.resolve(fileName);
        Files.write(testFile, "fake image data".getBytes());

        // Act
        ResponseEntity<Resource> response = domaineController.getDomaineImage(fileName);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @AfterEach
    void tearDown() {
        // Le répertoire temporaire sera automatiquement nettoyé par JUnit
    }
}