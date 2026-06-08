package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final CaseStudyRepository caseStudyRepository;
    private final AttemptRepository attemptRepository;
    private final ResetRequestRepository resetRequestRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm a", new Locale("es", "CO"));

    @Transactional(readOnly = true)
    public AdminStatsDto getAdminStats() {
        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long admins = userRepository.countByRole(UserRole.ADMIN);
        long enabled = userRepository.countByRoleAndEnabledTrue(UserRole.STUDENT);
        long disabled = userRepository.countByRoleAndEnabledFalse(UserRole.STUDENT);

        List<Attempt> all = attemptRepository.findAll();
        long passed = all.stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).count();
        long failed = all.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED || a.getStatus() == AttemptStatus.BLOCKED).count();
        long inProgress = all.stream().filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS).count();
        long blockedStudents = all.stream()
                .filter(a -> a.getStatus() == AttemptStatus.BLOCKED && !a.isArchived())
                .map(a -> a.getUser().getId()).distinct().count();

        long finished = passed + failed;
        double approvalRate = finished > 0 ? (passed * 100.0 / finished) : 0;

        Double avgC = attemptRepository.averageClinicalScore();
        Double avgE = attemptRepository.averageEthicalScore();
        Double avgN = attemptRepository.averageNormativeScore();

        return AdminStatsDto.builder()
                .totalStudents(totalStudents)
                .enabledStudents(enabled)
                .disabledStudents(disabled)
                .blockedStudents(blockedStudents)
                .totalAdmins(admins)
                .casesCreated(caseStudyRepository.count())
                .totalAttempts(all.size())
                .passedAttempts(passed)
                .failedAttempts(failed)
                .inProgressAttempts(inProgress)
                .approvalRate(round(approvalRate))
                .avgClinical(avgC != null ? round(avgC) : 0)
                .avgEthical(avgE != null ? round(avgE) : 0)
                .avgNormative(avgN != null ? round(avgN) : 0)
                .pendingResetRequests(resetRequestRepository.findByApprovedFalse().size())
                .leaderboard(buildLeaderboard())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> buildLeaderboard() {
        return userRepository.findByRole(UserRole.STUDENT).stream()
                .map(user -> {
                    List<Attempt> attempts = attemptRepository.findByUserOrderByStartedAtDesc(user).stream()
                            .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS).toList();
                    double best = attempts.stream().mapToDouble(Attempt::getTotalScore).max().orElse(0);
                    double avg = attempts.stream().mapToDouble(Attempt::getTotalScore).average().orElse(0);
                    long passed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).count();
                    return LeaderboardEntryDto.builder()
                            .userId(user.getId())
                            .fullName(user.getFullName())
                            .avatarUrl(user.getAvatarUrl())
                            .bestScore(round(best))
                            .averageScore(round(avg))
                            .attempts(attempts.size())
                            .passed(passed)
                            .build();
                })
                .sorted(Comparator.comparingDouble(LeaderboardEntryDto::getBestScore).reversed())
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResetRequestSummaryDto> listResetRequests(boolean pendingOnly) {
        List<ResetRequest> requests = pendingOnly
                ? resetRequestRepository.findByApprovedFalse()
                : resetRequestRepository.findAll();
        return requests.stream()
                .sorted(Comparator.comparing(ResetRequest::getRequestedAt).reversed())
                .map(this::toDto).toList();
    }

    @Transactional
    public MessageResponse approveReset(Long requestId) {
        ResetRequest request = resetRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        request.setApproved(true);
        resetRequestRepository.save(request);

        attemptRepository.findByUserAndCaseStudyOrderByAttemptNumberDesc(request.getUser(), request.getCaseStudy())
                .forEach(a -> {
                    a.setArchived(true);
                    attemptRepository.save(a);
                });

        notificationService.notify(request.getUser(), "Reinicio aprobado",
                "El profesor aprobó tu solicitud de reinicio para el caso \"" + request.getCaseStudy().getTitle() + "\".",
                NotificationType.RESET_APPROVED, "/cases/" + request.getCaseStudy().getId());

        return MessageResponse.builder().message("Solicitud aprobada y reintentos reiniciados").build();
    }

    @Transactional
    public MessageResponse rejectReset(Long requestId) {
        ResetRequest request = resetRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        notificationService.notify(request.getUser(), "Reinicio rechazado",
                "El profesor rechazó tu solicitud de reinicio para el caso \"" + request.getCaseStudy().getTitle() + "\".",
                NotificationType.WARNING, null);
        resetRequestRepository.delete(request);
        return MessageResponse.builder().message("Solicitud rechazada").build();
    }

    private ResetRequestSummaryDto toDto(ResetRequest r) {
        return ResetRequestSummaryDto.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .studentName(r.getUser().getFullName())
                .studentEmail(r.getUser().getEmail())
                .studentAvatar(r.getUser().getAvatarUrl())
                .caseId(r.getCaseStudy().getId())
                .caseTitle(r.getCaseStudy().getTitle())
                .approved(r.isApproved())
                .requestedAt(r.getRequestedAt().format(FORMATTER))
                .timeAgo(NotificationService.timeAgo(r.getRequestedAt()))
                .build();
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
