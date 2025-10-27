package com.marketplace.admin.service;

import com.marketplace.admin.dto.AdminDTO;
import com.marketplace.admin.entity.Admin;
import com.marketplace.admin.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;
@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    public AdminDTO login(String email, String motDePasse) {
        System.out.println("🔐 [LOGIN] Tentative de connexion avec email : " + email);

        // Récupérer admin depuis la BDD
        Optional<Admin> optionalAdmin = adminRepository.findByEmail(email);

        if (optionalAdmin.isEmpty()) {
            System.out.println("❌ [LOGIN] Aucun admin trouvé avec cet email : " + email);
            return null;  // ou throw exception si tu veux
        }

        Admin admin = optionalAdmin.get();

        System.out.println("✅ [LOGIN] Admin trouvé en base : " + admin.getEmail());
        System.out.println("📋 [LOGIN] Hash stocké en BDD : " + admin.getMotDePasse());

        // Vérifier le mot de passe
        boolean isPasswordMatch = passwordEncoder.matches(motDePasse, admin.getMotDePasse());
        if (!isPasswordMatch) {
            System.out.println("🚨 [LOGIN] Mot de passe incorrect pour l'email : " + email);
            throw new RuntimeException("Mot de passe incorrect");
        }

        System.out.println("🎉 [LOGIN] Mot de passe correct, connexion réussie pour : " + email);

        // Retourner DTO (ou ce que tu veux exposer)
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setEmail(admin.getEmail());
        adminDTO.setId(admin.getId());
        // ... compléter les autres champs si nécessaire
        return adminDTO;
    }

}
