package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaseAdminDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String level;
    private String imageUrl;
    private String contextQuote;
    private int estimatedMinutes;
    private double complexityStars;
    private List<String> competencies;
    private int questionCount;
    private long attemptsCount;
}
