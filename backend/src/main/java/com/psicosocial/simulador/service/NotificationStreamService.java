package com.psicosocial.simulador.service;

import com.psicosocial.simulador.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationStreamService {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(User user) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(user.getId(), id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(user.getId(), emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            cleanup.run();
        }
        return emitter;
    }

    public void publishUpdate(User user, long unreadCount) {
        List<SseEmitter> userEmitters = emitters.get(user.getId());
        if (userEmitters == null || userEmitters.isEmpty()) return;

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("update")
                        .data(Map.of("unreadCount", unreadCount)));
            } catch (IOException ex) {
                remove(user.getId(), emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId);
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
            log.debug("SSE emitter already closed for user {}", userId);
        }
    }
}
