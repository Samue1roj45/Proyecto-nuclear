package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.UserRole;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private UserRole role;
    private String avatarUrl;
    private Integer maxAttempts;
    private Boolean enabled;
    private String password;
}
