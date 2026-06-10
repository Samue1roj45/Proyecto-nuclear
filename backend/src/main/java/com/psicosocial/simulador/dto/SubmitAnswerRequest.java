package com.psicosocial.simulador.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long questionId;
    private Long optionId;
    private Integer elapsedSeconds;
}
