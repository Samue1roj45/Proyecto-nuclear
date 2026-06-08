package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationService {

    public static final double PASS_THRESHOLD = 60.0;

    private final CaseStudyRepository caseStudyRepository;
    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final ResetRequestRepository resetRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public CaseDetailDto getCaseDetail(Long caseId, User user) {
        CaseStudy caseStudy = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));

        List<Attempt> attempts = attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(user, caseStudy);
        CaseStatus status = DashboardService.resolveStatus(attempts, user.getMaxAttempts());
        boolean blocked = status == CaseStatus.BLOCKED || status == CaseStatus.FAILED;
        boolean resetPending = resetRequestRepository.findByUserAndCaseStudyAndApprovedFalse(user, caseStudy).isPresent();

        Attempt active = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        QuestionDto currentQuestion = null;
        int currentIndex = 0;
        Long activeAttemptId = null;

        if (active != null) {
            activeAttemptId = active.getId();
            currentIndex = active.getCurrentQuestionIndex();
            currentQuestion = toQuestionDto(getQuestionAt(caseStudy, currentIndex));
        } else if (!blocked && status == CaseStatus.AVAILABLE && !caseStudy.getQuestions().isEmpty()) {
            currentQuestion = toQuestionDto(caseStudy.getQuestions().get(0));
        }

        return CaseDetailDto.builder()
                .id(caseStudy.getId())
                .title(caseStudy.getTitle())
                .description(caseStudy.getDescription())
                .category(caseStudy.getCategory())
                .level(caseStudy.getLevel())
                .imageUrl(caseStudy.getImageUrl())
                .contextQuote(caseStudy.getContextQuote())
                .estimatedMinutes(caseStudy.getEstimatedMinutes())
                .complexityStars(caseStudy.getComplexityStars())
                .competencies(caseStudy.getCompetencies())
                .studentStatus(status)
                .attemptsUsed(attempts.size())
                .maxAttempts(user.getMaxAttempts())
                .blocked(blocked)
                .resetPending(resetPending)
                .activeAttemptId(activeAttemptId)
                .currentQuestionIndex(currentIndex)
                .totalQuestions(caseStudy.getQuestions().size())
                .currentQuestion(currentQuestion)
                .build();
    }

    @Transactional
    public CaseDetailDto startAttempt(Long caseId, User user) {
        CaseStudy caseStudy = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));

        List<Attempt> attempts = attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(user, caseStudy);
        CaseStatus status = DashboardService.resolveStatus(attempts, user.getMaxAttempts());
        if (status == CaseStatus.BLOCKED || status == CaseStatus.FAILED) {
            throw new RuntimeException("Caso bloqueado. Solicita reinicio de intentos.");
        }

        attemptRepository.findByUserAndCaseStudyAndStatus(user, caseStudy, AttemptStatus.IN_PROGRESS)
                .ifPresent(a -> { throw new RuntimeException("Ya tienes un intento en curso"); });

        if (attempts.size() >= user.getMaxAttempts()) {
            throw new RuntimeException("Intentos agotados");
        }

        Attempt attempt = Attempt.builder()
                .user(user)
                .caseStudy(caseStudy)
                .attemptNumber(attempts.size() + 1)
                .status(AttemptStatus.IN_PROGRESS)
                .currentQuestionIndex(0)
                .build();
        attemptRepository.save(attempt);
        return getCaseDetail(caseId, user);
    }

    @Transactional
    public CaseDetailDto submitAnswer(Long caseId, User user, SubmitAnswerRequest request) {
        CaseStudy caseStudy = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));

        Attempt attempt = attemptRepository.findByUserAndCaseStudyAndStatus(user, caseStudy, AttemptStatus.IN_PROGRESS)
                .orElseThrow(() -> new RuntimeException("No hay intento activo"));

        Question expected = getQuestionAt(caseStudy, attempt.getCurrentQuestionIndex());
        if (expected == null) {
            throw new RuntimeException("No hay más preguntas en este intento");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));

        if (!question.getId().equals(expected.getId())) {
            throw new RuntimeException("Debes responder la pregunta actual del caso");
        }
        if (!question.getCaseStudy().getId().equals(caseId)) {
            throw new RuntimeException("La pregunta no pertenece a este caso");
        }

        boolean alreadyAnswered = attempt.getAnswers().stream()
                .anyMatch(a -> a.getQuestion().getId().equals(question.getId()));
        if (alreadyAnswered) {
            throw new RuntimeException("Ya respondiste esta pregunta");
        }

        AnswerOption option = answerOptionRepository.findById(request.getOptionId())
                .orElseThrow(() -> new RuntimeException("Opción no encontrada"));
        if (!option.getQuestion().getId().equals(question.getId())) {
            throw new RuntimeException("La opción no pertenece a esta pregunta");
        }

        AttemptAnswer answer = AttemptAnswer.builder()
                .attempt(attempt)
                .question(question)
                .selectedOption(option)
                .correct(option.isCorrect())
                .build();
        attempt.getAnswers().add(answer);

        int nextIndex = attempt.getCurrentQuestionIndex() + 1;
        attempt.setCurrentQuestionIndex(nextIndex);

        if (nextIndex >= caseStudy.getQuestions().size()) {
            finalizeAttempt(attempt, caseStudy, user);
        }

        attemptRepository.save(attempt);
        return getCaseDetail(caseId, user);
    }

    @Transactional
    public MessageResponse requestReset(Long caseId, User user) {
        CaseStudy caseStudy = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));

        if (resetRequestRepository.findByUserAndCaseStudyAndApprovedFalse(user, caseStudy).isPresent()) {
            return MessageResponse.builder().message("Ya existe una solicitud pendiente").build();
        }

        resetRequestRepository.save(ResetRequest.builder().user(user).caseStudy(caseStudy).build());

        userRepository.findByRole(UserRole.ADMIN).forEach(admin ->
                notificationService.notify(admin, "Nueva solicitud de reinicio",
                        user.getFullName() + " solicitó reiniciar el caso \"" + caseStudy.getTitle() + "\".",
                        NotificationType.RESET_REQUEST, "/reset-requests"));

        return MessageResponse.builder().message("Solicitud de reinicio enviada al profesor").build();
    }

    private void finalizeAttempt(Attempt attempt, CaseStudy caseStudy, User user) {
        double clinical = scoreCategory(attempt, ScoreCategory.CLINICAL);
        double ethical = scoreCategory(attempt, ScoreCategory.ETHICAL);
        double normative = scoreCategory(attempt, ScoreCategory.NORMATIVE);
        double total = (clinical + ethical + normative) / 3.0;

        attempt.setClinicalScore(clinical);
        attempt.setEthicalScore(ethical);
        attempt.setNormativeScore(normative);
        attempt.setTotalScore(Math.round(total * 100.0) / 100.0);
        attempt.setCompletedAt(java.time.LocalDateTime.now());

        boolean passed = total >= PASS_THRESHOLD;
        if (passed) {
            attempt.setStatus(AttemptStatus.PASSED);
            notificationService.notify(user, "¡Caso aprobado!",
                    "Aprobaste el caso \"" + caseStudy.getTitle() + "\" con " + attempt.getTotalScore() + "%.",
                    NotificationType.CASE_COMPLETED, "/reports");
        } else {
            List<Attempt> all = attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(user, caseStudy);
            if (all.size() >= user.getMaxAttempts()) {
                attempt.setStatus(AttemptStatus.BLOCKED);
                notificationService.notify(user, "Caso bloqueado",
                        "Agotaste tus intentos en \"" + caseStudy.getTitle() + "\". Solicita un reinicio al profesor.",
                        NotificationType.WARNING, "/cases/" + caseStudy.getId());
            } else {
                attempt.setStatus(AttemptStatus.FAILED);
                notificationService.notify(user, "Caso no aprobado",
                        "Obtuviste " + attempt.getTotalScore() + "% en \"" + caseStudy.getTitle() + "\". Aún tienes intentos disponibles.",
                        NotificationType.INFO, "/cases/" + caseStudy.getId());
            }
        }
    }

    private double scoreCategory(Attempt attempt, ScoreCategory category) {
        List<AttemptAnswer> answers = attempt.getAnswers();
        long total = answers.stream()
                .filter(a -> a.getSelectedOption().getCategory() == category)
                .count();
        if (total == 0) return 0;
        long correct = answers.stream()
                .filter(a -> a.getSelectedOption().getCategory() == category && a.isCorrect())
                .count();
        return Math.round((correct * 100.0 / total) * 100.0) / 100.0;
    }

    private Question getQuestionAt(CaseStudy caseStudy, int index) {
        return caseStudy.getQuestions().stream()
                .sorted(Comparator.comparingInt(Question::getOrderIndex))
                .skip(index)
                .findFirst()
                .orElse(null);
    }

    private QuestionDto toQuestionDto(Question q) {
        if (q == null) return null;
        return QuestionDto.builder()
                .id(q.getId())
                .text(q.getText())
                .orderIndex(q.getOrderIndex())
                .sceneImageUrl(q.getSceneImageUrl())
                .options(q.getOptions().stream()
                        .sorted(Comparator.comparingInt(AnswerOption::getOrderIndex))
                        .map(o -> AnswerOptionDto.builder()
                                .id(o.getId())
                                .text(o.getText())
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .toList())
                .build();
    }
}
