package com.psicosocial.simulador.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReorderQuestionsRequest {
    private List<Long> questionIds;
}
