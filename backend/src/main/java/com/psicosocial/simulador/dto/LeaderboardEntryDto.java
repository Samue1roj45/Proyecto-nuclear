package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryDto {
    private Long userId;
    private String fullName;
    private String avatarUrl;
    private double bestScore;
    private double averageScore;
    private long attempts;
    private long passed;
}
