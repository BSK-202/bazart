package com.marketplace.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	//exemple: admin, user, expert
	@Column(unique = true, nullable = false, length = 32)
	private String name;
	
	private String description;

}
