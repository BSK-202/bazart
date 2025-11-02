package com.marketplace.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.admin.dto.AdminDTO;
import com.marketplace.admin.service.AdminService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collections;

public class AdminAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    public AdminAuthenticationFilter(AdminService adminService) {
        this.adminService = adminService;
        this.objectMapper = new ObjectMapper();
        setFilterProcessesUrl("/api/admins/login"); // ✅ Seulement pour cette route
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        try {
            // Lire les credentials admin depuis la requête
            AdminDTO adminDTO = objectMapper.readValue(request.getInputStream(), AdminDTO.class);

            System.out.println("🔐 [ADMIN FILTER] Tentative auth pour: " + adminDTO.getEmail());

            // Authentifier via AdminService
            AdminDTO authenticatedAdmin = adminService.login(adminDTO.getEmail(), adminDTO.getMotDePasse());

            if (authenticatedAdmin != null) {
                System.out.println("✅ [ADMIN FILTER] Auth réussie pour: " + authenticatedAdmin.getEmail());

                // Créer l'authentication Spring Security
                return new UsernamePasswordAuthenticationToken(
                        authenticatedAdmin.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            } else {
                System.out.println("❌ [ADMIN FILTER] Auth échouée");
                throw new AuthenticationException("Identifiants admin invalides") {};
            }

        } catch (IOException e) {
            throw new AuthenticationException("Erreur de lecture des credentials") {};
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        // ✅ Laisser AdminController gérer la réponse
        chain.doFilter(request, response);
    }
}