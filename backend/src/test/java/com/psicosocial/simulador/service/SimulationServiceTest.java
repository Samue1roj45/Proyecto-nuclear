package com.psicosocial.simulador.service;

import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {

    @Mock private CaseStudyRepository caseStudyRepository;
    @Mock private AttemptRepository attemptRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerOptionRepository answerOptionRepository;
    @Mock private ResetRequestRepository resetRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private CaseAccessService caseAccessService;

    @InjectMocks private SimulationService simulationService;

    private User student;
    private CaseStudy caseStudy;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("s@test.com").role(UserRole.STUDENT).maxAttempts(3).build();
        caseStudy = CaseStudy.builder().id(10L).title("Caso test").questions(new ArrayList<>()).build();
    }

    @Test
    void startAttempt_blocksWhenAlreadyPassed() {
        Attempt passed = Attempt.builder().status(AttemptStatus.PASSED).attemptNumber(1).archived(false).build();
        when(caseStudyRepository.findById(10L)).thenReturn(Optional.of(caseStudy));
        doNothing().when(caseAccessService).ensureCaseVisible(caseStudy, student);
        when(attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(student, caseStudy))
                .thenReturn(List.of(passed));

        Question q = Question.builder().id(1L).orderIndex(0).options(List.of()).build();
        caseStudy.setQuestions(List.of(q));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> simulationService.startAttempt(10L, student));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("aprobaste"));
    }

    @Test
    void startAttempt_createsAttemptWhenAvailable() {
        when(caseStudyRepository.findById(10L)).thenReturn(Optional.of(caseStudy));
        doNothing().when(caseAccessService).ensureCaseVisible(caseStudy, student);
        when(attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(student, caseStudy))
                .thenReturn(List.of());
        when(attemptRepository.findByUserAndCaseStudyAndStatus(student, caseStudy, AttemptStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resetRequestRepository.findByUserAndCaseStudyAndApprovedFalse(student, caseStudy))
                .thenReturn(Optional.empty());

        Question q = Question.builder().id(1L).orderIndex(0).text("P?").options(List.of()).build();
        caseStudy.setQuestions(List.of(q));

        assertDoesNotThrow(() -> simulationService.startAttempt(10L, student));
        verify(attemptRepository).save(any(Attempt.class));
    }
}
