package com.marketplace.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageVerificationService {

    private static final String FLASK_URL = "http://127.0.0.1:5000/verify-image";

    public boolean verifierImageAuthentique(MultipartFile image) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // ✅ Convertir directement MultipartFile en ByteArrayResource
            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename(); // indispensable pour que Flask reçoive bien le nom
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    FLASK_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response.getBody());

                double humanConfidence = json.path("probabilites").path("human").asDouble();
                System.out.println("🧠 Probabilité 'human' détectée : " + humanConfidence + "%");

                return humanConfidence >= 99.0;
            } else {
                System.err.println("❌ Erreur: réponse invalide du serveur Flask.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("🚨 Erreur lors de la vérification de l'image : " + e.getMessage());
            return false;
        }
    }
}
