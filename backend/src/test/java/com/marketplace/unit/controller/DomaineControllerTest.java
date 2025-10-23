package com.marketplace.unit.controller;

import com.marketplace.catalog.controller.DomaineController;
import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.service.DomaineService;
import com.marketplace.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DomaineController.class)
@Import(TestSecurityConfig.class)  //  MÊME IMPORT

class DomaineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DomaineService domaineService;


    @Test
    void getDomaines_ShouldReturnListOfDomaines() throws Exception {
        // Arrange
        Domaine domaine1 = new Domaine();
        domaine1.setIdDomaine(1L);
        domaine1.setNomDomaine("Informatique");
        domaine1.setImage("info.jpg");

        Domaine domaine2 = new Domaine();
        domaine2.setIdDomaine(2L);
        domaine2.setNomDomaine("Marketing");
        domaine2.setImage("marketing.jpg");

        List<Domaine> domaines = Arrays.asList(domaine1, domaine2);
        when(domaineService.getDomaines()).thenReturn(domaines);

        // Act & Assert
        mockMvc.perform(get("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nomDomaine").value("Informatique"))
                .andExpect(jsonPath("$[1].nomDomaine").value("Marketing"));

        verify(domaineService, times(1)).getDomaines();
    }

    @Test
    void getDomaines_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(domaineService.getDomaines()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/domaines")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(domaineService, times(1)).getDomaines();
    }

    @Test
    void getDomaineImage_WhenImageNotFound_ShouldReturn404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/domaines/images/notfound.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleCorsHeaders() throws Exception {
        // Arrange
        when(domaineService.getDomaines()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/domaines")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}