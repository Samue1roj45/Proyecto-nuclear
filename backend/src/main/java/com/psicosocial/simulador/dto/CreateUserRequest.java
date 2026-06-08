package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String fullName;
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    private UserRole role = UserRole.STUDENT;
    private String avatarUrl;
    private Integer maxAttempts = 3;
}
