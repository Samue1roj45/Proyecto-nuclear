package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CaseStudyRepository caseStudyRepository;
    private final AttemptRepository attemptRepository;
    private final ResetRequestRepository resetRequestRepository;

    public StudentDashboardDto getStudentDashboard(User user, String search) {
        List<CaseStudy> cases = search == null || search.isBlank()
                ? caseStudyRepository.findAll()
                : caseStudyRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(search, search);

        List<CaseCardDto> cards = cases.stream().map(c -> toCaseCard(c, user)).toList();

        return StudentDashboardDto.builder()
                .stats(buildStudentStats(user, cards))
                .cases(cards)
                .build();
    }

    private StudentStatsDto buildStudentStats(User user, List<CaseCardDto> cards) {
        long available = cards.stream()
                .filter(c -> c.getStudentStatus() == CaseStatus.AVAILABLE
                        || c.getStudentStatus() == CaseStatus.IN_PROGRESS)
                .count();
        long completed = cards.stream()
                .filter(c -> c.getStudentStatus() == CaseStatus.PASSED)
                .count();
        long inProgress = cards.stream()
                .filter(c -> c.getStudentStatus() == CaseStatus.IN_PROGRESS)
                .count();
        long blocked = cards.stream()
                .filter(c -> c.getStudentStatus() == CaseStatus.BLOCKED
                        || c.getStudentStatus() == CaseStatus.FAILED)
                .count();

        List<Attempt> finished = attemptRepository.findByUserOrderByStartedAtDesc(user).stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .toList();
        double best = finished.stream().mapToDouble(Attempt::getTotalScore).max().orElse(0);
        double avg = finished.stream().mapToDouble(Attempt::getTotalScore).average().orElse(0);

        return StudentStatsDto.builder()
                .availableCases(available)
                .completedCases(completed)
                .inProgressCases(inProgress)
                .blockedCases(blocked)
                .attemptsUsed(finished.size())
                .bestScore(Math.round(best * 100.0) / 100.0)
                .averageScore(Math.round(avg * 100.0) / 100.0)
                .build();
    }

    private CaseCardDto toCaseCard(CaseStudy caseStudy, User user) {
        List<Attempt> attempts = attemptRepository.findByUserAndCaseStudyAndArchivedFalseOrderByAttemptNumberDesc(user, caseStudy);
        int used = attempts.size();
        CaseStatus status = resolveStatus(attempts, user.getMaxAttempts());
        boolean resetPending = resetRequestRepository.findByUserAndCaseStudyAndApprovedFalse(user, caseStudy).isPresent();
        long enrolled = attemptRepository.findByCaseStudy(caseStudy).stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .count();

        return CaseCardDto.builder()
                .id(caseStudy.getId())
                .title(caseStudy.getTitle())
                .description(caseStudy.getDescription())
                .category(caseStudy.getCategory())
                .level(caseStudy.getLevel())
                .imageUrl(caseStudy.getImageUrl())
                .competencies(caseStudy.getCompetencies())
                .studentStatus(status)
                .attemptsUsed(used)
                .maxAttempts(user.getMaxAttempts())
                .resetPending(resetPending)
                .studentsEnrolled(enrolled)
                .build();
    }

    public static CaseStatus resolveStatus(List<Attempt> attempts, int maxAttempts) {
        if (attempts.isEmpty()) return CaseStatus.AVAILABLE;
        Attempt latest = attempts.get(0);
        return switch (latest.getStatus()) {
            case PASSED -> CaseStatus.PASSED;
            case IN_PROGRESS -> CaseStatus.IN_PROGRESS;
            case BLOCKED -> CaseStatus.BLOCKED;
            case FAILED -> attempts.size() >= maxAttempts ? CaseStatus.FAILED : CaseStatus.AVAILABLE;
        };
    }
}
