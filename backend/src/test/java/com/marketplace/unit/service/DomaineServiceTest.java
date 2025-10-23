package com.marketplace.unit.service;

import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.repository.DomaineRepository;
import com.marketplace.catalog.service.DomaineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomaineServiceTest {

    @Mock
    private DomaineRepository domaineRepository;

    @InjectMocks
    private DomaineService domaineService;

    @Test
    void getDomaines_ShouldReturnListOfDomaines() {
        // Arrange
        Domaine domaine1 = new Domaine();
        domaine1.setIdDomaine(1L);
        domaine1.setNomDomaine("Informatique");

        Domaine domaine2 = new Domaine();
        domaine2.setIdDomaine(2L);
        domaine2.setNomDomaine("Marketing");

        List<Domaine> expectedDomaines = Arrays.asList(domaine1, domaine2);

        // Mock du comportement du repository
        when(domaineRepository.findAll()).thenReturn(expectedDomaines);

        // Act
        List<Domaine> actualDomaines = domaineService.getDomaines();

        // Assert
        assertNotNull(actualDomaines);
        assertEquals(2, actualDomaines.size());
        assertEquals("Informatique", actualDomaines.get(0).getNomDomaine());
        assertEquals("Marketing", actualDomaines.get(1).getNomDomaine());

        // Vérification que la méthode du repository a été appelée
        verify(domaineRepository, times(1)).findAll();
    }

    @Test
    void getDomaines_WhenNoDomaines_ShouldReturnEmptyList() {
        // Arrange
        when(domaineRepository.findAll()).thenReturn(List.of());

        // Act
        List<Domaine> actualDomaines = domaineService.getDomaines();

        // Assert
        assertNotNull(actualDomaines);
        assertTrue(actualDomaines.isEmpty());
        verify(domaineRepository, times(1)).findAll();
    }
}