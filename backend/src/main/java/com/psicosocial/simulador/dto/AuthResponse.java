package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private String avatarUrl;
}
