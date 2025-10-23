package com.marketplace.integration.service;

import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.repository.DomaineRepository;
import com.marketplace.catalog.service.DomaineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(DomaineService.class) // Importez uniquement le service dont vous avez besoin
class DomaineServiceTest {

    @Autowired
    private DomaineService domaineService;


    @Test
    @Sql(scripts = {"/schema-test.sql", "/data-test.sql"})
    void testGetDomaines_ShouldReturnAllDomaines() {
        // Given - Les données sont chargées via data-test.sql

        // When
        List<Domaine> domaines = domaineService.getDomaines();

        // Then
        assertNotNull(domaines);
        assertEquals(4, domaines.size());

        // Vérifier le premier domaine
        Domaine premierDomaine = domaines.get(0);
        assertEquals(1L, premierDomaine.getIdDomaine());
        assertEquals("Technologie", premierDomaine.getNomDomaine());
        assertEquals("Domaines liés à la technologie et l'innovation", premierDomaine.getDescription());
        assertEquals("tech.jpg", premierDomaine.getImage());

        // Vérifier que tous les domaines ont les bonnes propriétés
        assertTrue(domaines.stream().anyMatch(d -> "Santé".equals(d.getNomDomaine())));
        assertTrue(domaines.stream().anyMatch(d -> "Éducation".equals(d.getNomDomaine())));
        assertTrue(domaines.stream().anyMatch(d -> "Sport".equals(d.getNomDomaine())));
    }

    @Test
    @Sql(scripts = {"/schema-test.sql"})
    void testGetDomaines_WhenNoData_ShouldReturnEmptyList() {
        // Given - Seul le schéma est créé, pas de données

        // When
        List<Domaine> domaines = domaineService.getDomaines();

        // Then
        assertNotNull(domaines);
        assertTrue(domaines.isEmpty());
    }
}