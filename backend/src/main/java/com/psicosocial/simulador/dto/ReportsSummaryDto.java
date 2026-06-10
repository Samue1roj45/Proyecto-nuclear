package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportsSummaryDto {
    private double approvalRate;
    private double approvalChange;
    private long casesAttempted;
    private double avgEthical;
    private double avgClinical;
    private double avgNormative;
    private List<AttemptSummaryDto> attempts;
    private long totalAttempts;
    private int page;
    private int pageSize;
}
