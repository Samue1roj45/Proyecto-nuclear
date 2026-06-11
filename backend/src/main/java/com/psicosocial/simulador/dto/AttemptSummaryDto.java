package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttemptSummaryDto {
    private Long id;
    private String studentName;
    private String studentEmail;
    private Long caseId;
    private String caseTitle;
    private int attemptNumber;
    private String date;
    private double totalScore;
    private String status;
    private double clinicalScore;
    private double ethicalScore;
    private double normativeScore;
}
