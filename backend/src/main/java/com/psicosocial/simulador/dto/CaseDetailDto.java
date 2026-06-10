package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.CaseStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CaseDetailDto {
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
    private CaseStatus studentStatus;
    private int attemptsUsed;
    private int maxAttempts;
    private boolean blocked;
    private boolean resetPending;
    private Long activeAttemptId;
    private int currentQuestionIndex;
    private int totalQuestions;
    private QuestionDto currentQuestion;
    private boolean timerEnabled;
    private int elapsedSeconds;
    private double passThreshold;
}
