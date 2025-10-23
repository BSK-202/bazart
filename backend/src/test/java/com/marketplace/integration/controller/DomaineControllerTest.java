package com.marketplace.integration.controller;

import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.repository.DomaineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class DomaineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DomaineRepository domaineRepository;

    @BeforeEach
    void setUp() {
        // Vérification que les données de test H2 sont bien chargées
        List<Domaine> domaines = domaineRepository.findAll();
        assertThat(domaines).hasSize(4);
    }

    @Test
    void getDomaines_ShouldReturnAllDomainesFromH2Database() throws Exception {
        // When & Then - Utiliser les données de data-test.sql
        mockMvc.perform(get("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].nomDomaine", is("Technologie")))
                .andExpect(jsonPath("$[0].description", is("Domaines liés à la technologie et l'innovation")))
                .andExpect(jsonPath("$[0].image", is("tech.jpg")))
                .andExpect(jsonPath("$[1].nomDomaine", is("Santé")))
                .andExpect(jsonPath("$[1].description", is("Domaines liés à la santé et bien-être")))
                .andExpect(jsonPath("$[1].image", is("sante.jpg")))
                .andExpect(jsonPath("$[2].nomDomaine", is("Éducation")))
                .andExpect(jsonPath("$[2].description", is("Domaines liés à l'éducation et formation")))
                .andExpect(jsonPath("$[2].image", is("education.jpg")))
                .andExpect(jsonPath("$[3].nomDomaine", is("Sport")))
                .andExpect(jsonPath("$[3].description", is("Domaines liés aux activités sportives")))
                .andExpect(jsonPath("$[3].image", is("sport.jpg")));
    }

    @Test
    void getDomaines_ShouldReturnCorrectDomainesStructure() throws Exception {
        // When & Then - Vérifier la structure avec les données H2
        mockMvc.perform(get("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idDomaine", is(1)))
                .andExpect(jsonPath("$[0].nomDomaine", is("Technologie")))
                .andExpect(jsonPath("$[1].idDomaine", is(2)))
                .andExpect(jsonPath("$[1].nomDomaine", is("Santé")))
                .andExpect(jsonPath("$[2].idDomaine", is(3)))
                .andExpect(jsonPath("$[2].nomDomaine", is("Éducation")))
                .andExpect(jsonPath("$[3].idDomaine", is(4)))
                .andExpect(jsonPath("$[3].nomDomaine", is("Sport")));
    }

    @Test
    void getDomaineImage_ShouldReturn404_WhenImageNotFound() throws Exception {
        // Given
        String nonExistentImage = "nonexistent.jpg";

        // When & Then
        mockMvc.perform(get("/api/domaines/images/" + nonExistentImage))
                .andExpect(status().isNotFound());
    }

    @Test
    void database_ShouldContainExactlyFourDomainesFromH2() {
        // Test direct sur le repository avec données H2
        List<Domaine> domaines = domaineRepository.findAll();
        assertThat(domaines).hasSize(4);

        // Vérifier le premier domaine (Technologie)
        Domaine technologie = domaines.stream()
                .filter(d -> d.getIdDomaine() == 1L)
                .findFirst()
                .orElseThrow();

        assertThat(technologie.getNomDomaine()).isEqualTo("Technologie");
        assertThat(technologie.getDescription()).isEqualTo("Domaines liés à la technologie et l'innovation");
        assertThat(technologie.getImage()).isEqualTo("tech.jpg");

        // Vérifier le deuxième domaine (Santé)
        Domaine sante = domaines.stream()
                .filter(d -> d.getIdDomaine() == 2L)
                .findFirst()
                .orElseThrow();

        assertThat(sante.getNomDomaine()).isEqualTo("Santé");
        assertThat(sante.getDescription()).isEqualTo("Domaines liés à la santé et bien-être");
        assertThat(sante.getImage()).isEqualTo("sante.jpg");
    }

    @Test
    void getDomaines_ShouldHaveCorrectCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/domaines")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void getDomaines_ShouldReturnJsonContentType() throws Exception {
        mockMvc.perform(get("/api/domaines"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}