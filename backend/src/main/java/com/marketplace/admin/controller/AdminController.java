// AdminController.java - VERSION SIMPLIFIÉE
package com.marketplace.admin.controller;

import com.marketplace.admin.dto.AdminDTO;
import com.marketplace.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminDTO adminDTO) {
        System.out.println("🔐 [CONTROLLER ADMIN] Tentative de connexion avec email : " + adminDTO.getEmail());

        AdminDTO admin = adminService.login(adminDTO.getEmail(), adminDTO.getMotDePasse());

        if (admin != null) {
            System.out.println("✅ [CONTROLLER ADMIN] Connexion réussie pour : " + admin.getEmail());
            return ResponseEntity.ok(admin);
        } else {
            System.out.println("❌ [CONTROLLER ADMIN] Échec de connexion");
            return ResponseEntity.status(401).body("Identifiants admin incorrects");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AdminDTO> register(@RequestBody AdminDTO adminDTO) {
        System.out.println("📩 [REGISTER ADMIN] Requête reçue : " + adminDTO.getEmail());
        AdminDTO savedAdmin = adminService.createAdmin(adminDTO);
        System.out.println("✅ [REGISTER ADMIN] Admin créé : " + savedAdmin.getId());
        return ResponseEntity.ok(savedAdmin);
    }
}