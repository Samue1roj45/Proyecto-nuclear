package com.psicosocial.simulador.dto;

import lombok.Data;

import java.util.List;

@Data
public class CaseRequest {
    private String title;
    private String description;
    private String category;
    private String level;
    private String imageUrl;
    private String contextQuote;
    private Integer estimatedMinutes;
    private Double complexityStars;
    private List<String> competencies;
    private Boolean timerEnabled;
}
