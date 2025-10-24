// SecurityConfig.java - VERSION CORRIGÉE
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
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/domaines/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/produits/**").permitAll()
                        .requestMatchers("/api/clients/*/upload-profile-image").permitAll()
                        .requestMatchers("/api/clients/*/profile-image").permitAll()
                        .requestMatchers("/api/commentaires/produit/*/count").permitAll()
                        .requestMatchers("/api/commentaires/produit/**").permitAll() // ✅ AUTORISER SANS AUTH
                        .requestMatchers("/api/interactions/**").authenticated() // ✅ Protéger les interactions
                        .requestMatchers("/api/commentaires/**").authenticated() // ✅ Protéger les commentaires
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/static/**", "/resources/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // ✅ ACTIVER LE FILTRE JWT

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow Angular and other origins
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://localhost:3000"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Client-Id",  // ✅ Ajouter X-Client-Id
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Client-Id"  // ✅ Exposer X-Client-Id
        ));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Cache duration for preflight requests
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