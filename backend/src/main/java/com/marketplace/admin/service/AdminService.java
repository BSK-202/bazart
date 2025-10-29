// AdminService.java - VERSION AVEC @Lazy
package com.marketplace.admin.service;

import com.marketplace.admin.dto.AdminDTO;
import com.marketplace.admin.entity.Admin;
import com.marketplace.admin.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    @Lazy  // ✅ AJOUTER @Lazy pour casser la dépendance circulaire
    private PasswordEncoder passwordEncoder;

    public AdminDTO login(String email, String motDePasse) {
        System.out.println("🔐 [LOGIN ADMIN SERVICE] Tentative de connexion avec email : " + email);

        // Récupérer admin depuis la BDD
        Optional<Admin> optionalAdmin = adminRepository.findByEmail(email);

        if (optionalAdmin.isEmpty()) {
            System.out.println("❌ [LOGIN ADMIN SERVICE] Aucun admin trouvé avec cet email : " + email);
            return null;
        }

        Admin admin = optionalAdmin.get();
        System.out.println("✅ [LOGIN ADMIN SERVICE] Admin trouvé en base : " + admin.getEmail());

        // Vérifier le mot de passe
        boolean isPasswordMatch = passwordEncoder.matches(motDePasse, admin.getMotDePasse());
        if (!isPasswordMatch) {
            System.out.println("🚨 [LOGIN ADMIN SERVICE] Mot de passe incorrect pour l'email : " + email);
            return null;
        }

        System.out.println("🎉 [LOGIN ADMIN SERVICE] Connexion réussie pour : " + email);

        // Retourner DTO
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setId(admin.getId());
        adminDTO.setEmail(admin.getEmail());


        return adminDTO;
    }

    public AdminDTO createAdmin(AdminDTO adminDTO) {
        System.out.println("🟢 [CREATE] Tentative de création d'un admin avec email : " + adminDTO.getEmail());
        Admin admin = new Admin();
        admin.setEmail(adminDTO.getEmail());
        admin.setMotDePasse(passwordEncoder.encode(adminDTO.getMotDePasse()));

        Admin saved = adminRepository.save(admin);
        System.out.println("✅ [CREATE] Admin créé avec ID : " + saved.getId());

        AdminDTO dto = new AdminDTO();
        dto.setId(saved.getId());
        dto.setEmail(saved.getEmail());
        return dto;
    }
}