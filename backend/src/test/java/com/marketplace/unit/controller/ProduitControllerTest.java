package com.marketplace.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.catalog.controller.ProduitController;
import com.marketplace.catalog.dto.ProduitDTO;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.entity.Categorie;
import com.marketplace.catalog.entity.ProduitImage;
import com.marketplace.user.entity.Client;
import com.marketplace.catalog.service.ProduitService;
import com.marketplace.catalog.service.CategorieService;
import com.marketplace.user.service.ClientService;
import com.marketplace.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProduitController.class)
@Import(TestSecurityConfig.class)  // ✅ CONFIGURATION DE SÉCURITÉ PARTAGÉE
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProduitService produitService;

    @MockitoBean
    private CategorieService categorieService;

    @MockitoBean
    private ClientService clientService;

    private Produit produit;
    private ProduitDTO produitDTO;
    private Categorie categorie;
    private Client vendeur;

    @BeforeEach
    void setUp() {
        // Setup categorie
        categorie = new Categorie();
        categorie.setIdCategorie(1L);
        categorie.setNomCategorie("Électronique");

        // Setup vendeur
        vendeur = new Client();
        vendeur.setIdclient(1L);
        vendeur.setNom("Dupont");
        vendeur.setPrenom("Jean");

        // Setup produit
        produit = new Produit();
        produit.setIdproduit(1L);
        produit.setNom("iPhone 13");
        produit.setDescription("Smartphone Apple");
        produit.setPrixDebut(500.0);
        produit.setPrixFin(800.0);
        produit.setEtat("en_attente");
        produit.setDatePublication(LocalDateTime.now());
        produit.setCategorie(categorie);
        produit.setVendeur(vendeur);

        ProduitImage image1 = new ProduitImage();
        image1.setUrl("image_1.jpg");
        image1.setProduit(produit);

        ProduitImage image2 = new ProduitImage();
        image2.setUrl("image_2.jpg");
        image2.setProduit(produit);

        produit.setImages(Arrays.asList(image1, image2));

        // Setup DTO
        produitDTO = new ProduitDTO();
        produitDTO.setId(1L);
        produitDTO.setNom("iPhone 13");
        produitDTO.setDescription("Smartphone Apple");
        produitDTO.setPrixDebut(500.0);
        produitDTO.setPrixFin(800.0);
        produitDTO.setEtat("en_attente");
        produitDTO.setCategorieNom("Électronique");
        produitDTO.setVendeurNom("Dupont");
        produitDTO.setImages(Arrays.asList("image_1.jpg", "image_2.jpg"));
    }

    // ✅ Test GET /api/produits/{id}
    @Test
    void getProduitById_ShouldReturnProduit() throws Exception {
        // Given
        when(produitService.getProduitById(1L)).thenReturn(Optional.of(produit));

        // When & Then
        mockMvc.perform(get("/api/produits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("iPhone 13"))
                .andExpect(jsonPath("$.categorieNom").value("Électronique"))
                .andExpect(jsonPath("$.vendeurNom").value("Dupont"));

        verify(produitService, times(1)).getProduitById(1L);
    }

    @Test
    void getProduitById_WhenNotFound_ShouldReturn404() throws Exception {
        // Given
        when(produitService.getProduitById(1L)).thenReturn(Optional.empty());

        // When & Then - Maintenant ça retourne bien 404
        mockMvc.perform(get("/api/produits/1"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    // Vérifier que c'est bien une ResponseStatusException avec status 404
                    assertTrue(result.getResolvedException() instanceof ResponseStatusException);
                    ResponseStatusException exception = (ResponseStatusException) result.getResolvedException();
                    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                    assertEquals("Produit non trouvé", exception.getReason());
                });

        verify(produitService, times(1)).getProduitById(1L);
    }
    // ✅ Test GET /api/produits/categorie/{idCategorie}/acceptes
    @Test
    void getProduitsAcceptesByCategorie_ShouldReturnList() throws Exception {
        // Given
        List<Produit> produits = Arrays.asList(produit);
        when(produitService.getProduitsAcceptesByCategorie(1L)).thenReturn(produits);

        // When & Then
        mockMvc.perform(get("/api/produits/categorie/1/acceptes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("iPhone 13"));

        verify(produitService, times(1)).getProduitsAcceptesByCategorie(1L);
    }

    // ✅ Test GET /api/produits/categorie/{idCategorie}/all
    @Test
    void getAllProduitsByCategorie_ShouldReturnAllProduits() throws Exception {
        // Given
        List<Produit> produits = Arrays.asList(produit);
        when(produitService.getProduitsByCategorie(1L)).thenReturn(produits);

        // When & Then
        mockMvc.perform(get("/api/produits/categorie/1/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("iPhone 13"));

        verify(produitService, times(1)).getProduitsByCategorie(1L);
    }

    // ✅ Test POST /api/produits (création avec images)
    @Test
    void createProduit_ShouldCreateSuccessfully() throws Exception {
        // Given
        ProduitDTO requestDTO = new ProduitDTO();
        requestDTO.setNom("MacBook Pro");
        requestDTO.setDescription("Ordinateur portable");
        requestDTO.setPrixDebut(1500.0);
        requestDTO.setPrixFin(2000.0);
        requestDTO.setCategorieId(1L);
        requestDTO.setVendeurId(1L);

        when(categorieService.getCategorieById(1L)).thenReturn(Optional.of(categorie));
        when(clientService.getClientById(1L)).thenReturn(Optional.of(vendeur));
        when(produitService.saveProduit(any(Produit.class))).thenReturn(produit);

        MockMultipartFile produitPart = new MockMultipartFile(
                "produit",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDTO)
        );

        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "image1.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "image2.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/produits")
                        .file(produitPart)
                        .file(image1)
                        .file(image2)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("iPhone 13"));

        verify(produitService, atLeastOnce()).saveProduit(any(Produit.class));
    }

    @Test
    void createProduit_WhenCategorieNotFound_ShouldReturnError() throws Exception {
        // Given
        ProduitDTO requestDTO = new ProduitDTO();
        requestDTO.setNom("MacBook Pro");
        requestDTO.setCategorieId(999L);
        requestDTO.setVendeurId(1L);

        when(categorieService.getCategorieById(999L)).thenReturn(Optional.empty());

        MockMultipartFile produitPart = new MockMultipartFile(
                "produit",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDTO)
        );

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "image.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/produits")
                        .file(produitPart)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());

        verify(produitService, never()).saveProduit(any(Produit.class));
    }

    @Test
    void createProduit_WhenVendeurNotFound_ShouldReturnError() throws Exception {
        // Given
        ProduitDTO requestDTO = new ProduitDTO();
        requestDTO.setNom("MacBook Pro");
        requestDTO.setCategorieId(1L);
        requestDTO.setVendeurId(999L);

        when(categorieService.getCategorieById(1L)).thenReturn(Optional.of(categorie));
        when(clientService.getClientById(999L)).thenReturn(Optional.empty());

        MockMultipartFile produitPart = new MockMultipartFile(
                "produit",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDTO)
        );

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "image.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/produits")
                        .file(produitPart)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());

        verify(produitService, never()).saveProduit(any(Produit.class));
    }

    // ✅ Test GET /api/produits/images/{produitId}/{fileName}
    @Test
    void getImage_WhenImageNotFound_ShouldReturn404() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/produits/images/1/notfound.jpg"))
                .andExpect(status().isNotFound());
    }

    // ✅ Test CORS headers
    @Test
    void shouldIncludeCorsHeaders() throws Exception {
        // Given
        when(produitService.getProduitById(1L)).thenReturn(Optional.of(produit));

        // When & Then
        mockMvc.perform(get("/api/produits/1")
                        .header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void createProduit_WhenRequiredFieldsMissing_ShouldReturnError() throws Exception {
        // Given - Missing categorieId
        ProduitDTO requestDTO = new ProduitDTO();
        requestDTO.setNom("MacBook Pro");
        requestDTO.setVendeurId(1L);
        // categorieId is null

        MockMultipartFile produitPart = new MockMultipartFile(
                "produit",
                "",
                "application/json",
                objectMapper.writeValueAsBytes(requestDTO)
        );

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "image.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/produits")
                        .file(produitPart)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }
}