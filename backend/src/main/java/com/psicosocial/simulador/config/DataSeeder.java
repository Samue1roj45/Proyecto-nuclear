package com.psicosocial.simulador.config;

import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseStudyRepository caseStudyRepository;
    private final AttemptRepository attemptRepository;
    private final NotificationRepository notificationRepository;
    private final ResetRequestRepository resetRequestRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CASE_IMAGE =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCjkwslr8U4Zg1XklPZ8d_U9BW9rWhEnZYMBjtS5_-jA2teROU9uoahJgmOj-HYyXQZFZbsuCesZtkUV00y_E6HolZnla_fESU_TPVQZO0j9bZm9LLVOMrdoqPHcS9dM1_ikMbSBPqOjGGsP_Ug6XWafrw7ii308Fbu8evZE9XELTW_7zfrr0beKA5FYo-iqVibHGFO0xYNrjF0GxgZnqeYsj7gffBjt8WKnAXSeMKYSc_1dsXGj9LF9qepZSdq-FSie5gDZ_Hxqe8";

    private static final String SCENE_IMAGE =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCchZK36IRMZfIHa7H0GybJx4uklkGvEk1ZusCDArfzWuXcW4BfpZgavBPxZe7PoVcIzInL7oYPbZkvbbX9U30S6rZ_AggghjAA3W0bKmhXfSArVuMXJxk3OtW3AjikEx8ax1Jn-O-pQmEteWsDaj1vpS0qFHQgmOudDhzu_3ZCxKyFhg95IbNgUJvQz0aA_gs3NCenWjsbAxYFSCozt_83vHzlW4rS4DlO6hNTp45M1D3WJefyHfSUDqDXxhBIOMiHfnfjfAIvbtg";

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        User admin = userRepository.save(User.builder()
                .fullName("Profesor Admin")
                .email("admin@simulador.com")
                .password(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build());

        User student = userRepository.save(User.builder()
                .fullName("Estudiante Prueba")
                .email("estudiante@simulador.com")
                .password(passwordEncoder.encode("estudiante123"))
                .role(UserRole.STUDENT)
                .maxAttempts(3)
                .build());

        CaseStudy case1 = CaseStudy.builder()
                .title("Caso 1: Violencia familiar y tentativa de feminicidio")
                .description("Simulación orientada a desafiar el juicio clínico, ético y normativo del estudiante de psicología, integrando atención en crisis, restablecimiento de derechos, violencia basada en género, riesgo de feminicidio, duelo y activación de rutas de atención.")
                .category("Violencia basada en género")
                .level("Nivel Intermedio")
                .imageUrl(CASE_IMAGE)
                .contextQuote("Usted llega a la unidad residencial después de un llamado de emergencia...")
                .estimatedMinutes(45)
                .complexityStars(3.5)
                .competencies(List.of(
                        "Lectura de contexto",
                        "Toma de decisiones bajo presión",
                        "Aplicación de normativa colombiana",
                        "Intervención ética",
                        "Primeros Auxilios Psicológicos",
                        "Valoración de riesgo",
                        "Activación de rutas institucionales"
                ))
                .build();

        Question q1 = Question.builder()
                .caseStudy(case1)
                .text("¿Cuál es su primera acción al ingresar?")
                .orderIndex(0)
                .sceneImageUrl(SCENE_IMAGE)
                .build();

        q1.setOptions(List.of(
                option(q1, "Entrevistar por separado a los implicados de inmediato.", 0, false, ScoreCategory.CLINICAL),
                option(q1, "Asegurar el entorno y verificar integridad física de la víctima.", 1, true, ScoreCategory.CLINICAL),
                option(q1, "Llamar a la policía nacional antes de mediar.", 2, false, ScoreCategory.NORMATIVE)
        ));

        Question q2 = Question.builder()
                .caseStudy(case1)
                .text("La víctima manifiesta miedo extremo. ¿Cuál es la intervención más adecuada?")
                .orderIndex(1)
                .sceneImageUrl(SCENE_IMAGE)
                .build();

        q2.setOptions(List.of(
                option(q2, "Aplicar primeros auxilios psicológicos y validar emociones.", 0, true, ScoreCategory.ETHICAL),
                option(q2, "Solicitar que relate los hechos con detalle inmediato.", 1, false, ScoreCategory.CLINICAL),
                option(q2, "Informar al agresor sobre la denuncia en curso.", 2, false, ScoreCategory.ETHICAL)
        ));

        Question q3 = Question.builder()
                .caseStudy(case1)
                .text("¿Qué ruta institucional debe activar según la normativa colombiana?")
                .orderIndex(2)
                .sceneImageUrl(SCENE_IMAGE)
                .build();

        q3.setOptions(List.of(
                option(q3, "Comisaría de familia y ruta de atención integral VBG.", 0, true, ScoreCategory.NORMATIVE),
                option(q3, "Solo derivación a psicología clínica privada.", 1, false, ScoreCategory.NORMATIVE),
                option(q3, "Esperar consentimiento del agresor para activar rutas.", 2, false, ScoreCategory.ETHICAL)
        ));

        case1.setQuestions(List.of(q1, q2, q3));
        caseStudyRepository.save(case1);

        Attempt attempt1 = Attempt.builder()
                .user(student)
                .caseStudy(case1)
                .attemptNumber(1)
                .status(AttemptStatus.FAILED)
                .totalScore(16.67)
                .clinicalScore(33.33)
                .ethicalScore(0)
                .normativeScore(16.67)
                .currentQuestionIndex(3)
                .startedAt(LocalDateTime.of(2026, 5, 27, 17, 59))
                .completedAt(LocalDateTime.of(2026, 5, 27, 17, 59))
                .build();

        Attempt attempt2 = Attempt.builder()
                .user(student)
                .caseStudy(case1)
                .attemptNumber(2)
                .status(AttemptStatus.FAILED)
                .totalScore(50.0)
                .clinicalScore(50.0)
                .ethicalScore(70.0)
                .normativeScore(30.0)
                .currentQuestionIndex(3)
                .startedAt(LocalDateTime.of(2026, 5, 27, 20, 20))
                .completedAt(LocalDateTime.of(2026, 5, 27, 20, 20))
                .build();

        Attempt attempt3 = Attempt.builder()
                .user(student)
                .caseStudy(case1)
                .attemptNumber(3)
                .status(AttemptStatus.BLOCKED)
                .totalScore(33.33)
                .clinicalScore(33.33)
                .ethicalScore(33.33)
                .normativeScore(33.33)
                .currentQuestionIndex(3)
                .startedAt(LocalDateTime.of(2026, 5, 28, 10, 0))
                .completedAt(LocalDateTime.of(2026, 5, 28, 10, 30))
                .build();

        attemptRepository.saveAll(List.of(attempt1, attempt2, attempt3));

        seedExtraData(admin, student, case1);
    }

    private void seedExtraData(User admin, User student, CaseStudy case1) {
        String[][] students = {
                {"María Fernanda López", "maria.lopez@simulador.com"},
                {"Carlos Andrés Ruiz", "carlos.ruiz@simulador.com"},
                {"Valentina Gómez", "valentina.gomez@simulador.com"},
                {"Juan David Torres", "juan.torres@simulador.com"},
                {"Laura Sofía Méndez", "laura.mendez@simulador.com"},
                {"Andrés Felipe Castro", "andres.castro@simulador.com"}
        };

        int idx = 0;
        for (String[] s : students) {
            boolean enabled = idx != 4;
            User u = userRepository.save(User.builder()
                    .fullName(s[0])
                    .email(s[1])
                    .password(passwordEncoder.encode("estudiante123"))
                    .role(UserRole.STUDENT)
                    .maxAttempts(3)
                    .enabled(enabled)
                    .build());

            int attemptsToCreate = (idx % 3) + 1;
            for (int n = 1; n <= attemptsToCreate; n++) {
                double clinical = 30 + (idx * 7 + n * 11) % 65;
                double ethical = 40 + (idx * 5 + n * 9) % 55;
                double normative = 25 + (idx * 9 + n * 7) % 70;
                double total = Math.round(((clinical + ethical + normative) / 3.0) * 100.0) / 100.0;
                AttemptStatus status;
                if (total >= 60) status = AttemptStatus.PASSED;
                else if (n >= 3) status = AttemptStatus.BLOCKED;
                else status = AttemptStatus.FAILED;

                attemptRepository.save(Attempt.builder()
                        .user(u)
                        .caseStudy(case1)
                        .attemptNumber(n)
                        .status(status)
                        .totalScore(total)
                        .clinicalScore(clinical)
                        .ethicalScore(ethical)
                        .normativeScore(normative)
                        .currentQuestionIndex(3)
                        .startedAt(LocalDateTime.now().minusDays(idx + 1L).minusHours(n))
                        .completedAt(LocalDateTime.now().minusDays(idx + 1L).minusHours(n).plusMinutes(35))
                        .build());
            }

            if (idx == 1) {
                resetRequestRepository.save(ResetRequest.builder().user(u).caseStudy(case1).build());
                notificationRepository.save(Notification.builder()
                        .user(admin)
                        .title("Nueva solicitud de reinicio")
                        .message(u.getFullName() + " solicitó reiniciar el caso \"" + case1.getTitle() + "\".")
                        .type(NotificationType.RESET_REQUEST)
                        .link("/reset-requests")
                        .build());
            }
            idx++;
        }

        notificationRepository.save(Notification.builder()
                .user(student)
                .title("Caso bloqueado")
                .message("Agotaste tus intentos en \"" + case1.getTitle() + "\". Solicita un reinicio al profesor.")
                .type(NotificationType.WARNING)
                .link("/cases/" + case1.getId())
                .read(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(student)
                .title("Bienvenido a Misión Psicosocial")
                .message("Recorre el caso, responde con criterio clínico y consulta tus reportes.")
                .type(NotificationType.INFO)
                .link("/dashboard")
                .read(true)
                .build());

        notificationRepository.save(Notification.builder()
                .user(admin)
                .title("Resumen académico")
                .message("Tienes nuevos intentos registrados de varios estudiantes para revisar.")
                .type(NotificationType.INFO)
                .link("/reports")
                .build());
    }

    private AnswerOption option(Question q, String text, int order, boolean correct, ScoreCategory cat) {
        return AnswerOption.builder()
                .question(q)
                .text(text)
                .orderIndex(order)
                .correct(correct)
                .category(cat)
                .weight(1.0)
                .build();
    }
}
