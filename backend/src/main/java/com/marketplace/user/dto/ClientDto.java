package com.marketplace.user.dto;

import lombok.*;
import java.util.Set;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
	private Long idclient;
	private String nom;
	private String prenom;
	private String email;
	private String tel;
	private String pays;
	private String ville;
	private String photoprofil;
	private Set<String> roles;
	private boolean enabled;

	public String getFullName() {
		return (nom != null ? nom : "") + (prenom != null ? " " + prenom : "");
	}

	public Long getId() {
		return idclient;
	}

	public void setId(Long id) {
		this.idclient = id;
	}
}