package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public MessageResponse deleteQuestion(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada"));
        CaseStudy c = q.getCaseStudy();
        c.getQuestions().removeIf(item -> item.getId().equals(questionId));
        caseStudyRepository.save(c);
        return MessageResponse.builder().message("Pregunta eliminada").build();
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
