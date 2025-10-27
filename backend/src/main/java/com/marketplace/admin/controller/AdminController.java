package com.marketplace.admin.controller;

import com.marketplace.admin.dto.AdminDTO;
import com.marketplace.admin.service.AdminService;
import com.marketplace.catalog.service.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private ProduitService produitService;



    @PostMapping("/register")
    public AdminDTO register(@RequestBody AdminDTO adminDTO) {
        System.out.println("📩 [REGISTER] Requête reçue pour enregistrement d'un nouvel admin : " + adminDTO.getEmail());
        AdminDTO savedAdmin = adminService.createAdmin(adminDTO);
        System.out.println("✅ [REGISTER] Admin créé avec succès : " + savedAdmin.getId() + " (" + savedAdmin.getEmail() + ")");
        return savedAdmin;
    }

    @PostMapping("/login")
    public AdminDTO login(@RequestBody AdminDTO adminDTO) {
        System.out.println("🔐 [LOGIN] Tentative de connexion avec email : " + adminDTO.getEmail());
        try {
            AdminDTO admin = adminService.login(adminDTO.getEmail(), adminDTO.getMotDePasse());
            if (admin == null) {
                System.out.println("❌ [LOGIN] Échec de connexion : email ou mot de passe incorrect.");
                return null;
            }
            System.out.println("✅ [LOGIN] Connexion réussie pour l'admin : " + admin.getEmail());
            return admin;
        } catch (Exception e) {
            System.out.println("🚨 [LOGIN] Erreur inattendue : " + e.getMessage());
            throw e;
        }
    }
}
