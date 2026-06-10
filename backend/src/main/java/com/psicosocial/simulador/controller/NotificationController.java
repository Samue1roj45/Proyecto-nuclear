package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.MessageResponse;
import com.psicosocial.simulador.dto.NotificationDto;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.security.CustomUserDetailsService;
import com.psicosocial.simulador.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.psicosocial.simulador.service.NotificationStreamService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;
    private final CustomUserDetailsService userDetailsService;

    private User current(Authentication auth) {
        return userDetailsService.getUser(auth.getName());
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.list(current(auth), unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(current(auth))));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        return notificationStreamService.subscribe(current(auth));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markRead(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(notificationService.markRead(id, current(auth)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllRead(Authentication auth) {
        return ResponseEntity.ok(notificationService.markAllRead(current(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(notificationService.delete(id, current(auth)));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> clearAll(Authentication auth) {
        return ResponseEntity.ok(notificationService.clearAll(current(auth)));
    }
}
