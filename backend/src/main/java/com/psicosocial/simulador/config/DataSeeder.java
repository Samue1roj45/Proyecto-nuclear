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

        case1.setTimerEnabled(true);

        Question q1 = Question.builder()
                .caseStudy(case1)
                .text("¿Cuál es su primera acción al ingresar?")
                .orderIndex(0)
                .sceneImageUrl(SCENE_IMAGE)
                .sceneTitle("Escena 1 · Llegada de emergencia")
                .sceneSubtitle("Unidad residencial · Noche")
                .sceneHint("Acércate a la víctima para evaluar su estado.")
                .npcLabel("Víctima")
                .build();

        q1.setOptions(List.of(
                option(q1, "Entrevistar por separado a los implicados de inmediato.", 0, false, ScoreCategory.CLINICAL,
                        "Separar sin asegurar el entorno puede re-victimizar y aumentar el riesgo."),
                option(q1, "Asegurar el entorno y verificar integridad física de la víctima.", 1, true, ScoreCategory.CLINICAL,
                        "Correcto: la seguridad física y emocional es la prioridad en crisis VBG."),
                option(q1, "Llamar a la policía nacional antes de mediar.", 2, false, ScoreCategory.NORMATIVE,
                        "La policía puede ser necesaria, pero no antes de garantizar contención y evaluación.")
        ));

        Question q2 = Question.builder()
                .caseStudy(case1)
                .text("La víctima manifiesta miedo extremo. ¿Cuál es la intervención más adecuada?")
                .orderIndex(1)
                .sceneImageUrl(SCENE_IMAGE)
                .sceneTitle("Escena 2 · Sala de contención")
                .sceneSubtitle("Interior · Zona segura")
                .sceneHint("La víctima muestra miedo extremo. Acércate con cuidado.")
                .npcLabel("Víctima")
                .build();

        q2.setOptions(List.of(
                option(q2, "Aplicar primeros auxilios psicológicos y validar emociones.", 0, true, ScoreCategory.ETHICAL,
                        "Correcto: PAP estabilizan emocionalmente sin exigir relato inmediato."),
                option(q2, "Solicitar que relate los hechos con detalle inmediato.", 1, false, ScoreCategory.CLINICAL,
                        "Exigir relato detallado en crisis puede re-traumatizar a la víctima."),
                option(q2, "Informar al agresor sobre la denuncia en curso.", 2, false, ScoreCategory.ETHICAL,
                        "Informar al agresor pone en riesgo a la víctima y viola confidencialidad.")
        ));

        Question q3 = Question.builder()
                .caseStudy(case1)
                .text("¿Qué ruta institucional debe activar según la normativa colombiana?")
                .orderIndex(2)
                .sceneImageUrl(SCENE_IMAGE)
                .sceneTitle("Escena 3 · Activación de rutas")
                .sceneSubtitle("Puesto de coordinación")
                .sceneHint("Ve al panel de rutas institucionales.")
                .npcLabel(null)
                .build();

        q3.setOptions(List.of(
                option(q3, "Comisaría de familia y ruta de atención integral VBG.", 0, true, ScoreCategory.NORMATIVE,
                        "Correcto: activa la ruta integral de protección según Ley 1257 y normativa vigente."),
                option(q3, "Solo derivación a psicología clínica privada.", 1, false, ScoreCategory.NORMATIVE,
                        "La atención privada no activa las rutas de protección estatal obligatorias."),
                option(q3, "Esperar consentimiento del agresor para activar rutas.", 2, false, ScoreCategory.ETHICAL,
                        "El consentimiento del agresor no es requisito para activar rutas de protección.")
        ));

        case1.setQuestions(List.of(q1, q2, q3));
        caseStudyRepository.save(case1);

        CaseStudy case2 = buildCase2();
        CaseStudy case3 = buildCase3();
        caseStudyRepository.saveAll(List.of(case2, case3));

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

    private CaseStudy buildCase2() {
        CaseStudy c = CaseStudy.builder()
                .title("Caso 2: Acoso laboral y salud mental")
                .description("Simulación sobre intervención psicosocial en contexto laboral con señales de acoso, burnout y riesgo suicida.")
                .category("Salud ocupacional")
                .level("Nivel Básico")
                .imageUrl(CASE_IMAGE)
                .contextQuote("Un trabajador solicita apoyo tras meses de presión constante de su jefe...")
                .estimatedMinutes(30)
                .complexityStars(2.5)
                .timerEnabled(false)
                .competencies(List.of("Detección de riesgo", "Confidencialidad", "Derivación"))
                .build();

        Question q1 = Question.builder().caseStudy(c).text("¿Cuál es su primer paso al recibir la consulta?")
                .orderIndex(0).sceneTitle("Escena 1 · Consulta inicial").sceneSubtitle("Oficina de recursos humanos")
                .sceneHint("Escucha activa sin juzgar.").npcLabel("Trabajador").build();
        q1.setOptions(List.of(
                option(q1, "Escuchar con empatía y evaluar nivel de riesgo.", 0, true, ScoreCategory.CLINICAL, "Correcto: la contención y evaluación de riesgo son prioritarias."),
                option(q1, "Solicitar pruebas del acoso antes de intervenir.", 1, false, ScoreCategory.ETHICAL, "Exigir pruebas puede invalidar la experiencia del consultante."),
                option(q1, "Contactar al jefe inmediatamente.", 2, false, ScoreCategory.NORMATIVE, "Contactar al presunto agresor sin protocolo puede agravar la situación.")
        ));

        Question q2 = Question.builder().caseStudy(c).text("El trabajador menciona ideación suicida leve. ¿Qué hace?")
                .orderIndex(1).sceneTitle("Escena 2 · Evaluación de riesgo").sceneSubtitle("Sala de entrevista")
                .sceneHint("Evalúa factores de protección y riesgo.").npcLabel("Trabajador").build();
        q2.setOptions(List.of(
                option(q2, "Activar protocolo de riesgo y no dejarlo solo.", 0, true, ScoreCategory.CLINICAL, "Correcto: ideación suicida requiere protocolo inmediato."),
                option(q2, "Agendar cita para la próxima semana.", 1, false, ScoreCategory.ETHICAL, "Posponer con ideación suicida es negligencia."),
                option(q2, "Informar a compañeros para que lo vigilen.", 2, false, ScoreCategory.ETHICAL, "Violación grave de confidencialidad.")
        ));

        c.setQuestions(List.of(q1, q2));
        return c;
    }

    private CaseStudy buildCase3() {
        CaseStudy c = CaseStudy.builder()
                .title("Caso 3: Intervención comunitaria post-conflicto")
                .description("Toma de decisiones en un barrio afectado por violencia, priorizando enfoque comunitario y participación.")
                .category("Psicología comunitaria")
                .level("Nivel Avanzado")
                .imageUrl(CASE_IMAGE)
                .contextQuote("La comunidad solicita apoyo tras un enfrentamiento que dejó familias desplazadas...")
                .estimatedMinutes(50)
                .complexityStars(4.0)
                .timerEnabled(true)
                .competencies(List.of("Trabajo comunitario", "Mediación", "Enfoque cultural"))
                .build();

        Question q1 = Question.builder().caseStudy(c).text("¿Cómo inicia la intervención comunitaria?")
                .orderIndex(0).sceneTitle("Escena 1 · Asamblea comunitaria").sceneSubtitle("Centro comunitario")
                .sceneHint("Escucha las voces de la comunidad.").npcLabel("Líder comunitario").build();
        q1.setOptions(List.of(
                option(q1, "Diagnóstico participativo con actores clave.", 0, true, ScoreCategory.CLINICAL, "Correcto: el enfoque participativo legitima la intervención."),
                option(q1, "Imponer un plan elaborado desde la universidad.", 1, false, ScoreCategory.ETHICAL, "Imponer desde afuera reproduce dinámicas de desposeimiento."),
                option(q1, "Evitar involucrar a líderes informales.", 2, false, ScoreCategory.NORMATIVE, "Los líderes informales son clave en contextos comunitarios.")
        ));

        Question q2 = Question.builder().caseStudy(c).text("Dos familias en conflicto piden mediación. ¿Qué hace?")
                .orderIndex(1).sceneTitle("Escena 2 · Mediación").sceneSubtitle("Salón comunal")
                .sceneHint("Neutralidad y seguridad emocional.").npcLabel("Familias").build();
        q2.setOptions(List.of(
                option(q2, "Mediación con reglas claras y espacio seguro.", 0, true, ScoreCategory.ETHICAL, "Correcto: la mediación requiere neutralidad y reglas."),
                option(q2, "Tomar partido por la familia más afectada.", 1, false, ScoreCategory.ETHICAL, "Perder neutralidad invalida la mediación."),
                option(q2, "Derivar a la fuerza pública de inmediato.", 2, false, ScoreCategory.NORMATIVE, "La fuerza pública no es primera línea en mediación comunitaria.")
        ));

        Question q3 = Question.builder().caseStudy(c).text("¿Cómo documenta y reporta la intervención?")
                .orderIndex(2).sceneTitle("Escena 3 · Informe").sceneSubtitle("Coordinación institucional")
                .sceneHint("Documenta con enfoque ético.").build();
        q3.setOptions(List.of(
                option(q3, "Informe con consentimiento y enfoque de derechos.", 0, true, ScoreCategory.NORMATIVE, "Correcto: documentar con consentimiento protege a la comunidad."),
                option(q3, "Publicar nombres en redes para visibilizar.", 1, false, ScoreCategory.ETHICAL, "Exponer datos viola confidencialidad y puede estigmatizar."),
                option(q3, "No documentar para proteger al equipo.", 2, false, ScoreCategory.NORMATIVE, "La omisión de registro dificulta continuidad y rendición de cuentas.")
        ));

        c.setQuestions(List.of(q1, q2, q3));
        return c;
    }

    private AnswerOption option(Question q, String text, int order, boolean correct, ScoreCategory cat, String feedback) {
        return AnswerOption.builder()
                .question(q)
                .text(text)
                .orderIndex(order)
                .correct(correct)
                .category(cat)
                .feedback(feedback)
                .weight(1.0)
                .build();
    }
}
