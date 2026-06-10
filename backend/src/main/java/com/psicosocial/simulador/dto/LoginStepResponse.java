package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginStepResponse {
    private LoginStep step;
    private String message;
    private String detail;
    private String email;
    private String studentName;
    private String accessSession;
    private Integer expiresInMinutes;
    private String expiresAt;
    private Boolean emailSent;
    private AuthResponse auth;
}
