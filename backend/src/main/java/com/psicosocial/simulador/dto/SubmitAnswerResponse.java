package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitAnswerResponse {
    private CaseDetailDto caseDetail;
    private AnswerFeedbackDto feedback;
    private AttemptResultDto result;
}
