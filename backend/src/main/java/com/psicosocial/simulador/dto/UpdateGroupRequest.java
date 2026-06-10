package com.psicosocial.simulador.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateGroupRequest {
    private String name;
    private String description;
    private List<Long> studentIds;
    private List<Long> caseIds;
}
