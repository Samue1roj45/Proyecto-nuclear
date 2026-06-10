package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.AccessCodeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessStatusResponse {
    private AccessCodeStatus status;
    private String message;
    private String detail;
    private boolean canEnterCode;
    private boolean canRetry;
    private Integer expiresInMinutes;
    private String expiresAt;
    private String studentName;
}
