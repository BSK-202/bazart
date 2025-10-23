package com.marketplace.auth.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String email;
    private String password;
    private String googleToken;

    // Champs pour l'inscription
    private String nom;
    private String prenom;
    private String tel;
    private String pays;
    private String ville;
    private String photoprofil;

    public String getFullName() {
        return (nom != null ? nom : "") + (prenom != null ? " " + prenom : "");
    }
}