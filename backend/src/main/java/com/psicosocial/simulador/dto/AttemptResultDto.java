package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttemptResultDto {
    private boolean passed;
    private double totalScore;
    private double clinicalScore;
    private double ethicalScore;
    private double normativeScore;
    private Long attemptId;
}
