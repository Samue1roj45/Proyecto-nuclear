package com.psicosocial.simulador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String resetCode;
    @NotBlank @Size(min = 6)
    private String newPassword;
}
