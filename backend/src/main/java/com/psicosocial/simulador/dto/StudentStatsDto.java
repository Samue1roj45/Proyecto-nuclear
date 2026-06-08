package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentStatsDto {
    private long availableCases;
    private long completedCases;
    private long inProgressCases;
    private long blockedCases;
    private long attemptsUsed;
    private double bestScore;
    private double averageScore;
}
