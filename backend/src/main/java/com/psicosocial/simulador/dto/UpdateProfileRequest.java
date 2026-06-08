package com.psicosocial.simulador.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String avatarUrl;
}
