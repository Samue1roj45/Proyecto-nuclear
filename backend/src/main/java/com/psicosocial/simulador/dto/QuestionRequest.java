package com.psicosocial.simulador.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    private String text;
    private String sceneImageUrl;
    private String sceneTitle;
    private String sceneSubtitle;
    private String sceneHint;
    private String npcLabel;
    private List<OptionRequest> options;

    @Data
    public static class OptionRequest {
        private String text;
        private boolean correct;
        private String category;
        private String feedback;
    }
}
