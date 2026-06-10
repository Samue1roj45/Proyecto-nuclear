package com.psicosocial.simulador.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaPatch {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void patchNotificationTypeConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS CONSTRAINT_5");
        } catch (Exception ex) {
            log.debug("Constraint CONSTRAINT_5 no presente: {}", ex.getMessage());
        }

        try {
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CONSTRAINTS " +
                            "WHERE UPPER(TABLE_NAME) = 'NOTIFICATIONS' AND CONSTRAINT_TYPE = 'CHECK'"
            );
            for (Map<String, Object> row : constraints) {
                String name = String.valueOf(row.get("CONSTRAINT_NAME"));
                jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT " + name);
                log.info("Eliminada restricción CHECK {} en notifications", name);
            }
        } catch (Exception ex) {
            log.debug("No se pudo ajustar constraints de notifications: {}", ex.getMessage());
        }
    }
}
