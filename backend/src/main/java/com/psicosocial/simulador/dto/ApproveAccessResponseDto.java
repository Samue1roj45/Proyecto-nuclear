package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApproveAccessResponseDto {
    private String message;
    private String code;
    private String expiresAt;
    private boolean emailSent;
    private int expiresInMinutes;
}
