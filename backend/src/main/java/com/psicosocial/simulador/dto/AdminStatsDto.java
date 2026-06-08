package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminStatsDto {
    private long totalStudents;
    private long enabledStudents;
    private long disabledStudents;
    private long blockedStudents;
    private long totalAdmins;
    private long casesCreated;
    private long totalAttempts;
    private long passedAttempts;
    private long failedAttempts;
    private long inProgressAttempts;
    private double approvalRate;
    private double avgClinical;
    private double avgEthical;
    private double avgNormative;
    private long pendingResetRequests;
    private List<LeaderboardEntryDto> leaderboard;
}
