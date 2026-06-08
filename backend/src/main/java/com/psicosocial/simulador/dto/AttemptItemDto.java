package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttemptItemDto {
    private String questionText;
    private String selectedAnswer;
    private boolean correct;
    private String category;
}
