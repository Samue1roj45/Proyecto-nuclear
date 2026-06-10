package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.AccessCodeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccessRequestSummaryDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private AccessCodeStatus status;
    private String requestedAt;
    private String timeAgo;
    private String approvedByName;
    private String approvedAt;
    private String expiresAt;
}
