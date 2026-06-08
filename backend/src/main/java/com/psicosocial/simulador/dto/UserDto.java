package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.AuthProvider;
import com.psicosocial.simulador.model.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private String avatarUrl;
    private boolean enabled;
    private int maxAttempts;
    private boolean blocked;
    private long totalAttempts;
    private long passedAttempts;
    private double averageScore;
    private AuthProvider authProvider;
}
