// JwtAuthenticationFilter.java - VERSION AMÉLIORÉE
package com.marketplace.core.security;

import com.marketplace.user.service.ClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ClientService clientService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ClientService clientService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.clientService = clientService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ✅ IGNORER COMPLÈTEMENT toutes les routes admin
        if (requestURI.startsWith("/api/admins/")) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ IGNORER les routes d'authentification
        if (requestURI.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }
        // Ignorer création et lecture des experts sans JWT
        if (requestURI.startsWith("/api/experts")) {
            chain.doFilter(request, response);
            return;
        }


        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                username = jwtTokenProvider.getUsernameFromToken(token);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = clientService.loadUserByUsername(username);

            if (userDetails != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }


        chain.doFilter(request, response);
    }
}