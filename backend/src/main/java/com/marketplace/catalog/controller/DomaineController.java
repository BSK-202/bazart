package com.marketplace.catalog.controller;

import com.marketplace.catalog.entity.Domaine;
import com.marketplace.catalog.service.DomaineService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/domaines")
@CrossOrigin(origins = "*", allowedHeaders = "*", exposedHeaders = "*")
public class DomaineController {

    private final DomaineService domaineService;

    // Image storage directory
    private final String UPLOAD_DIR = "assets/domaines/";

    public DomaineController(DomaineService domaineService) {
        this.domaineService = domaineService;
    }
    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getDomaineImage(@PathVariable String fileName) {
        try {
            Path imagePath = Paths.get(UPLOAD_DIR + fileName);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                        // 🚫 AUCUN CACHE
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            } else {
                System.err.println("❌ Domain image not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Error reading domain image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    @GetMapping
    public List<Domaine> getDomaines() {
        return domaineService.getDomaines();
    }
}