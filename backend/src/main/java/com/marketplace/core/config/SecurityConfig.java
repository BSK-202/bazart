// SecurityConfig.java - VERSION SANS DÉPENDANCE CIRCULAIRE
package com.marketplace.core.config;

import com.marketplace.core.security.JwtAuthenticationFilter;
import com.marketplace.core.security.JwtTokenProvider;
import com.marketplace.user.service.ClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    // ✅ SUPPRIMER la dépendance AdminService du constructeur

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ClientService clientService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, clientService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ✅ Endpoints PUBLIC (sans authentification)
                		.requestMatchers("/ws-notif/**").permitAll()
                		
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admins/login").permitAll() // Admin login public
                        .requestMatchers("/api/admins/register").permitAll() // Admin register public
                        .requestMatchers("/api/domaines/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/produits/**").permitAll()
                        .requestMatchers("/api/clients/*/photo").permitAll() // ✅ AJOUTÉ
                        .requestMatchers("/api/clients/*/upload-profile-image").permitAll()
                        .requestMatchers("/api/clients/**").permitAll()

                        .requestMatchers("/api/clients/*/profile-image").permitAll()
                        .requestMatchers("/api/commentaires/produit/*/count").permitAll()
                        .requestMatchers("/api/commentaires/produit/**").permitAll()
                        .requestMatchers("/api/clients/images/**").permitAll()
                        .requestMatchers("/api/experts/*/upload-signatures").permitAll()
                        .requestMatchers("/api/experts/**").permitAll()

                        .requestMatchers("/api/experts").permitAll()
                        .requestMatchers("/api/produits/*/start-auction").permitAll()


                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/static/**", "/resources/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // ✅ Endpoints ADMIN PROTÉGÉS - Utiliser authenticated() au lieu de hasRole()
                        .requestMatchers("/api/admins/**").authenticated()

                        // ✅ Endpoints CLIENT PROTÉGÉS
                        .requestMatchers("/api/interactions/**").authenticated()
                        .requestMatchers("/api/commentaires/**").authenticated()
                        .requestMatchers("/api/wallet/**").authenticated()

                        .anyRequest().authenticated()
                )
                // ✅ SEULEMENT LE FILTRE JWT (supprimer le filtre admin)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:4200", "http://127.0.0.1:4200", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Client-Id", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "X-Client-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}