package com.psicosocial.simulador.dto;

import com.psicosocial.simulador.model.NotificationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private String link;
    private boolean read;
    private String createdAt;
    private String timeAgo;
}
