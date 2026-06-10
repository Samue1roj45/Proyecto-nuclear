package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.MessageResponse;
import com.psicosocial.simulador.dto.NotificationDto;
import com.psicosocial.simulador.model.Notification;
import com.psicosocial.simulador.model.NotificationType;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamService notificationStreamService;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm a", new Locale("es", "CO"));

    public void notify(User user, String title, String message, NotificationType type, String link) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .build());
        notificationStreamService.publishUpdate(user, unreadCount(user));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(User user, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user)
                : notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return notifications.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public MessageResponse markRead(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No autorizado");
        }
        n.setRead(true);
        notificationRepository.save(n);
        notificationStreamService.publishUpdate(user, unreadCount(user));
        return MessageResponse.builder().message("Notificación marcada como leída").build();
    }

    @Transactional
    public MessageResponse markAllRead(User user) {
        notificationRepository.markAllRead(user);
        notificationStreamService.publishUpdate(user, unreadCount(user));
        return MessageResponse.builder().message("Todas las notificaciones marcadas como leídas").build();
    }

    @Transactional
    public MessageResponse delete(Long id, User user) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No autorizado");
        }
        notificationRepository.delete(n);
        notificationStreamService.publishUpdate(user, unreadCount(user));
        return MessageResponse.builder().message("Notificación eliminada").build();
    }

    @Transactional
    public MessageResponse clearAll(User user) {
        notificationRepository.deleteAllByUser(user);
        notificationStreamService.publishUpdate(user, unreadCount(user));
        return MessageResponse.builder().message("Notificaciones eliminadas").build();
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .link(n.getLink())
                .read(n.isRead())
                .createdAt(n.getCreatedAt().format(FORMATTER))
                .timeAgo(timeAgo(n.getCreatedAt()))
                .build();
    }

    static String timeAgo(LocalDateTime time) {
        Duration d = Duration.between(time, LocalDateTime.now());
        long minutes = d.toMinutes();
        if (minutes < 1) return "hace un momento";
        if (minutes < 60) return "hace " + minutes + " min";
        long hours = d.toHours();
        if (hours < 24) return "hace " + hours + " h";
        long days = d.toDays();
        if (days < 30) return "hace " + days + " d";
        return time.format(FORMATTER);
    }
}
