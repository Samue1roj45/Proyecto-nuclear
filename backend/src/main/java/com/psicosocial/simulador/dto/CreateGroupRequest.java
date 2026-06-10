package com.psicosocial.simulador.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank
    private String name;
    private String description;
    private List<Long> studentIds;
    private List<Long> caseIds;
}
