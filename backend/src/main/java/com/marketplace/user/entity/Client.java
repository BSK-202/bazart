package com.marketplace.user.entity;

import com.marketplace.auth.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Client")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Client {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idclient;

	private String nom;
	private String prenom;

	@Column(unique = true)
	private String email;

	private String motdepasse;
	private String tel;
	private String pays;
	private String ville;
	private String photoprofil;

	public void setDateinscription(LocalDateTime dateinscription) {
		this.dateinscription = dateinscription;
	}

	private LocalDateTime dateinscription;

	// Champs de sécurité
	@Column(nullable = false)
	private boolean enabled = true;

	@Column(unique = true)
	private String googleId;
    @Column(nullable = false)
    private boolean emailVerified = false;


	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "clients_roles",
			joinColumns = @JoinColumn(name = "client_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	@Builder.Default
	private Set<Role> roles = new HashSet<>();

	@PrePersist
	public void prePersist() {
		if (dateinscription == null) {
			dateinscription = LocalDateTime.now();
		}
		if (this.roles == null || this.roles.isEmpty()) {
			this.roles = new HashSet<>();
		}
	}

	public void addRole(Role role) {
		if (this.roles == null) {
			this.roles = new HashSet<>();
		}
		this.roles.add(role);
	}

	// Méthodes de compatibilité
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