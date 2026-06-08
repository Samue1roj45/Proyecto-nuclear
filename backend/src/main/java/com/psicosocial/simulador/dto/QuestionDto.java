package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionDto {
    private Long id;
    private String text;
    private int orderIndex;
    private String sceneImageUrl;
    private List<AnswerOptionDto> options;
}
