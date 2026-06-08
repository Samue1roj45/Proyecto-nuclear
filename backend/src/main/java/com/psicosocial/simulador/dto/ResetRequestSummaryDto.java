package com.psicosocial.simulador.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetRequestSummaryDto {
    private Long id;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private Long caseId;
    private String caseTitle;
    private boolean approved;
    private String requestedAt;
    private String timeAgo;
}
