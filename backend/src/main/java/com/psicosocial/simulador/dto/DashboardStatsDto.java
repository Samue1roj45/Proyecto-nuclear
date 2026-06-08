package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    private long totalStudents;
    private long enabledStudents;
    private long casesCreated;
    private long blockedStudents;
    private double approvalRate;
}
