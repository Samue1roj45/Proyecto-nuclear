package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentDashboardDto {
    private StudentStatsDto stats;
    private List<CaseCardDto> cases;
}
