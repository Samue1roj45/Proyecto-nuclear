package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HelpSectionDto {
    private String id;
    private String title;
    private String icon;
    private List<HelpItemDto> items;

    @Data
    @Builder
    public static class HelpItemDto {
        private String question;
        private String answer;
    }
}
