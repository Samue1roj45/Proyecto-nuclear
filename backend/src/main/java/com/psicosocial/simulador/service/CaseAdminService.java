package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseAdminService {

    private final CaseStudyRepository caseStudyRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;

    @Transactional(readOnly = true)
    public List<CaseAdminDto> listCases(String search) {
        List<CaseStudy> cases = search == null || search.isBlank()
                ? caseStudyRepository.findAll()
                : caseStudyRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(search, search);
        return cases.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionAdminDto> listQuestions(Long caseId) {
        CaseStudy c = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        return c.getQuestions().stream()
                .sorted(Comparator.comparingInt(Question::getOrderIndex))
                .map(this::toQuestionDto)
                .toList();
    }

    @Transactional
    public CaseAdminDto createCase(CaseRequest req) {
        CaseStudy c = CaseStudy.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .level(req.getLevel() != null ? req.getLevel() : "Nivel Intermedio")
                .imageUrl(req.getImageUrl())
                .contextQuote(req.getContextQuote())
                .estimatedMinutes(req.getEstimatedMinutes() != null ? req.getEstimatedMinutes() : 45)
                .complexityStars(req.getComplexityStars() != null ? req.getComplexityStars() : 3.0)
                .timerEnabled(req.getTimerEnabled() != null && req.getTimerEnabled())
                .competencies(req.getCompetencies() != null ? req.getCompetencies() : new ArrayList<>())
                .build();
        return toDto(caseStudyRepository.save(c));
    }

    @Transactional
    public CaseAdminDto updateCase(Long id, CaseRequest req) {
        CaseStudy c = caseStudyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        if (req.getTitle() != null) c.setTitle(req.getTitle());
        if (req.getDescription() != null) c.setDescription(req.getDescription());
        if (req.getCategory() != null) c.setCategory(req.getCategory());
        if (req.getLevel() != null) c.setLevel(req.getLevel());
        if (req.getImageUrl() != null) c.setImageUrl(req.getImageUrl());
        if (req.getContextQuote() != null) c.setContextQuote(req.getContextQuote());
        if (req.getEstimatedMinutes() != null) c.setEstimatedMinutes(req.getEstimatedMinutes());
        if (req.getComplexityStars() != null) c.setComplexityStars(req.getComplexityStars());
        if (req.getTimerEnabled() != null) c.setTimerEnabled(req.getTimerEnabled());
        if (req.getCompetencies() != null) c.setCompetencies(req.getCompetencies());
        return toDto(caseStudyRepository.save(c));
    }

    private CaseAdminDto toDto(CaseStudy c) {
        return CaseAdminDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .category(c.getCategory())
                .level(c.getLevel())
                .imageUrl(c.getImageUrl())
                .contextQuote(c.getContextQuote())
                .estimatedMinutes(c.getEstimatedMinutes())
                .complexityStars(c.getComplexityStars())
                .competencies(c.getCompetencies())
                .questionCount(c.getQuestions() != null ? c.getQuestions().size() : 0)
                .attemptsCount(attemptRepository.countByCaseStudy(c))
                .timerEnabled(c.isTimerEnabled())
                .build();
    }

    @Transactional
    public MessageResponse deleteCase(Long id) {
        CaseStudy c = caseStudyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        caseStudyRepository.delete(c);
        return MessageResponse.builder().message("Caso eliminado").build();
    }

    @Transactional
    public Question addQuestion(Long caseId, QuestionRequest req) {
        CaseStudy c = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        Question q = Question.builder()
                .caseStudy(c)
                .text(req.getText())
                .sceneImageUrl(req.getSceneImageUrl())
                .sceneTitle(req.getSceneTitle())
                .sceneSubtitle(req.getSceneSubtitle())
                .sceneHint(req.getSceneHint())
                .npcLabel(req.getNpcLabel())
                .orderIndex(c.getQuestions().size())
                .build();
        List<AnswerOption> options = new ArrayList<>();
        int idx = 0;
        if (req.getOptions() != null) {
            for (QuestionRequest.OptionRequest o : req.getOptions()) {
                options.add(AnswerOption.builder()
                        .question(q)
                        .text(o.getText())
                        .correct(o.isCorrect())
                        .category(parseCategory(o.getCategory()))
                        .feedback(o.getFeedback())
                        .orderIndex(idx++)
                        .weight(1.0)
                        .build());
            }
        }
        q.setOptions(options);
        c.getQuestions().add(q);
        caseStudyRepository.save(c);
        return q;
    }

    @Transactional
    public QuestionAdminDto updateQuestion(Long questionId, QuestionRequest req) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));
        if (req.getText() != null) q.setText(req.getText());
        if (req.getSceneImageUrl() != null) q.setSceneImageUrl(req.getSceneImageUrl());
        if (req.getSceneTitle() != null) q.setSceneTitle(req.getSceneTitle());
        if (req.getSceneSubtitle() != null) q.setSceneSubtitle(req.getSceneSubtitle());
        if (req.getSceneHint() != null) q.setSceneHint(req.getSceneHint());
        if (req.getNpcLabel() != null) q.setNpcLabel(req.getNpcLabel());

        if (req.getOptions() != null && !req.getOptions().isEmpty()) {
            q.getOptions().clear();
            int idx = 0;
            for (QuestionRequest.OptionRequest o : req.getOptions()) {
                q.getOptions().add(AnswerOption.builder()
                        .question(q)
                        .text(o.getText())
                        .correct(o.isCorrect())
                        .category(parseCategory(o.getCategory()))
                        .feedback(o.getFeedback())
                        .orderIndex(idx++)
                        .weight(1.0)
                        .build());
            }
        }
        caseStudyRepository.save(q.getCaseStudy());
        return toQuestionDto(q);
    }

    @Transactional
    public MessageResponse reorderQuestions(Long caseId, ReorderQuestionsRequest req) {
        CaseStudy c = caseStudyRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));
        if (req.getQuestionIds() == null || req.getQuestionIds().isEmpty()) {
            throw new RuntimeException("Debes indicar el orden de las preguntas");
        }

        var questionMap = c.getQuestions().stream()
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));

        int index = 0;
        for (Long id : req.getQuestionIds()) {
            Question q = questionMap.get(id);
            if (q == null) {
                throw new RuntimeException("La pregunta " + id + " no pertenece a este caso");
            }
            q.setOrderIndex(index++);
        }
        caseStudyRepository.save(c);
        return MessageResponse.builder().message("Orden actualizado").build();
    }

    @Transactional
    public MessageResponse deleteQuestion(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));
        CaseStudy c = q.getCaseStudy();
        c.getQuestions().removeIf(item -> item.getId().equals(questionId));
        reindexQuestions(c);
        caseStudyRepository.save(c);
        return MessageResponse.builder().message("Pregunta eliminada").build();
    }

    private void reindexQuestions(CaseStudy c) {
        List<Question> sorted = c.getQuestions().stream()
                .sorted(Comparator.comparingInt(Question::getOrderIndex))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setOrderIndex(i);
        }
    }

    private QuestionAdminDto toQuestionDto(Question q) {
        return QuestionAdminDto.builder()
                .id(q.getId())
                .text(q.getText())
                .orderIndex(q.getOrderIndex())
                .sceneImageUrl(q.getSceneImageUrl())
                .sceneTitle(q.getSceneTitle())
                .sceneSubtitle(q.getSceneSubtitle())
                .sceneHint(q.getSceneHint())
                .npcLabel(q.getNpcLabel())
                .options(q.getOptions().stream()
                        .sorted(Comparator.comparingInt(AnswerOption::getOrderIndex))
                        .map(o -> QuestionAdminDto.OptionAdminDto.builder()
                                .id(o.getId())
                                .text(o.getText())
                                .correct(o.isCorrect())
                                .category(o.getCategory() != null ? o.getCategory().name() : "CLINICAL")
                                .feedback(o.getFeedback())
                                .orderIndex(o.getOrderIndex())
                                .build())
                        .toList())
                .build();
    }

    private ScoreCategory parseCategory(String value) {
        if (value == null) return ScoreCategory.CLINICAL;
        try {
            return ScoreCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ScoreCategory.CLINICAL;
        }
    }
}
