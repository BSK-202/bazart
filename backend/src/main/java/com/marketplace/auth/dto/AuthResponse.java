package com.marketplace.auth.dto;

import com.marketplace.user.dto.ClientDto;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType; //Bearer
    private ClientDto client; // Changé de UserDto à ClientDto
}