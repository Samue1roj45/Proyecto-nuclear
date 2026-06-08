package com.psicosocial.simulador.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthFacebookRequest {
    @NotBlank
    private String accessToken;
}
