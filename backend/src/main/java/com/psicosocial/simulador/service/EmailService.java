package com.psicosocial.simulador.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.from-name:Misión Psicosocial}")
    private String fromName;

    public boolean isConfigured() {
        return mailSender != null && from != null && !from.isBlank();
    }

    public void sendPasswordResetCode(String toEmail, String fullName, String code) {
        if (!isConfigured()) {
            throw new RuntimeException(
                    "El envío de correos no está configurado. Configura MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD y MAIL_FROM en el backend."
            );
        }

        String subject = "Código de recuperación — Misión Psicosocial";
        String body = """
                Hola %s,

                Recibimos una solicitud para restablecer tu contraseña en Misión Psicosocial.

                Tu código de recuperación es: %s

                Este código es válido durante 60 minutos.

                Si no solicitaste este cambio, puedes ignorar este correo.

                — Equipo Misión Psicosocial
                """.formatted(fullName, code);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("%s <%s>".formatted(fromName, from));
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Código de recuperación enviado a {}", toEmail);
        } catch (Exception ex) {
            log.error("Error enviando correo a {}", toEmail, ex);
            throw new RuntimeException("No se pudo enviar el correo. Verifica la configuración SMTP.");
        }
    }
}
