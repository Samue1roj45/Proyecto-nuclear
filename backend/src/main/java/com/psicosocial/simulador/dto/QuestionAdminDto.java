package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionAdminDto {
    private Long id;
    private String text;
    private int orderIndex;
    private String sceneImageUrl;
    private String sceneTitle;
    private String sceneSubtitle;
    private String sceneHint;
    private String npcLabel;
    private List<OptionAdminDto> options;

    @Data
    @Builder
    public static class OptionAdminDto {
        private Long id;
        private String text;
        private boolean correct;
        private String category;
        private String feedback;
        private int orderIndex;
    }
}
