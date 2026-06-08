package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaseCardDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String level;
    private String imageUrl;
    private List<String> competencies;
    private CaseStatus studentStatus;
    private int attemptsUsed;
    private int maxAttempts;
    private boolean resetPending;
    private long studentsEnrolled;
}
