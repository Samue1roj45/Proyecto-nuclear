package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.Attempt;
import com.psicosocial.simulador.model.AttemptAnswer;
import com.psicosocial.simulador.model.AttemptStatus;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.model.UserRole;
import com.psicosocial.simulador.repository.AttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final AttemptRepository attemptRepository;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm a", new Locale("es", "CO"));

    @Transactional(readOnly = true)
    public ReportsSummaryDto getReports(User requester, String search, String status, String sort, int page, int pageSize) {
        boolean studentScope = requester.getRole() == UserRole.STUDENT;
        List<Attempt> attempts = filterAttempts(requester, studentScope, search, status);

        Comparator<Attempt> comparator = switch (sort == null ? "date" : sort) {
            case "score" -> Comparator.comparingDouble(Attempt::getTotalScore).reversed();
            case "scoreAsc" -> Comparator.comparingDouble(Attempt::getTotalScore);
            case "name" -> Comparator.comparing(a -> a.getUser().getFullName());
            default -> Comparator.comparing(Attempt::getStartedAt).reversed();
        };

        List<Attempt> sorted = attempts.stream().sorted(comparator).toList();
        long filteredTotal = sorted.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, pageSize), 100);

        List<AttemptSummaryDto> list = sorted.stream()
                .skip((long) safePage * safeSize)
                .limit(safeSize)
                .map(this::toSummary)
                .toList();

        double avgClinical;
        double avgEthical;
        double avgNormative;
        long passed;
        long totalFinished;
        long casesAttempted;

        if (studentScope) {
            List<Attempt> own = attempts;
            avgClinical = own.stream().mapToDouble(Attempt::getClinicalScore).average().orElse(0);
            avgEthical = own.stream().mapToDouble(Attempt::getEthicalScore).average().orElse(0);
            avgNormative = own.stream().mapToDouble(Attempt::getNormativeScore).average().orElse(0);
            passed = own.stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).count();
            totalFinished = own.size();
            casesAttempted = own.stream().map(a -> a.getCaseStudy().getId()).distinct().count();
        } else {
            Double c = attemptRepository.averageClinicalScore();
            Double e = attemptRepository.averageEthicalScore();
            Double n = attemptRepository.averageNormativeScore();
            avgClinical = c != null ? c : 0;
            avgEthical = e != null ? e : 0;
            avgNormative = n != null ? n : 0;
            passed = attemptRepository.countByStatus(AttemptStatus.PASSED);
            totalFinished = passed
                    + attemptRepository.countByStatus(AttemptStatus.FAILED)
                    + attemptRepository.countByStatus(AttemptStatus.BLOCKED);
            casesAttempted = attemptRepository.countDistinctCasesAttempted();
        }

        double approvalRate = totalFinished > 0 ? (passed * 100.0 / totalFinished) : 0;
        double approvalChange = computeApprovalChange(requester, studentScope);

        return ReportsSummaryDto.builder()
                .approvalRate(round(approvalRate))
                .approvalChange(round(approvalChange))
                .casesAttempted(casesAttempted)
                .avgEthical(round(avgEthical))
                .avgClinical(round(avgClinical))
                .avgNormative(round(avgNormative))
                .attempts(list)
                .totalAttempts(filteredTotal)
                .page(safePage)
                .pageSize(safeSize)
                .build();
    }

    private double computeApprovalChange(User requester, boolean studentScope) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime weekAgo = now.minusDays(7);
        java.time.LocalDateTime twoWeeksAgo = now.minusDays(14);

        List<Attempt> recent = attemptRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .filter(a -> !studentScope || a.getUser().getId().equals(requester.getId()))
                .filter(a -> a.getCompletedAt() != null)
                .toList();

        double thisWeek = approvalRate(recent.stream().filter(a -> !a.getCompletedAt().isBefore(weekAgo)).toList());
        double lastWeek = approvalRate(recent.stream()
                .filter(a -> !a.getCompletedAt().isBefore(twoWeeksAgo) && a.getCompletedAt().isBefore(weekAgo))
                .toList());
        return thisWeek - lastWeek;
    }

    private double approvalRate(List<Attempt> attempts) {
        if (attempts.isEmpty()) return 0;
        long passed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).count();
        return passed * 100.0 / attempts.size();
    }

    @Transactional(readOnly = true)
    public String exportCsv(User requester, String search, String status) {
        boolean studentScope = requester.getRole() == UserRole.STUDENT;
        List<Attempt> attempts = filterAttempts(requester, studentScope, search, status);
        StringBuilder sb = new StringBuilder();
        sb.append("Estudiante,Correo,Caso,Intento,Fecha,Puntaje,Clinico,Etico,Normativo,Estado\n");
        for (Attempt a : attempts) {
            String date = a.getCompletedAt() != null ? a.getCompletedAt().format(FORMATTER) : a.getStartedAt().format(FORMATTER);
            sb.append(escape(a.getUser().getFullName())).append(",")
                    .append(escape(a.getUser().getEmail())).append(",")
                    .append(escape(a.getCaseStudy().getTitle())).append(",")
                    .append(a.getAttemptNumber()).append(",")
                    .append(escape(date)).append(",")
                    .append(a.getTotalScore()).append(",")
                    .append(a.getClinicalScore()).append(",")
                    .append(a.getEthicalScore()).append(",")
                    .append(a.getNormativeScore()).append(",")
                    .append(a.getStatus().name()).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public AttemptSummaryDto getAttemptDetail(Long attemptId, User requester) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Intento no encontrado"));
        ensureOwnership(attempt, requester);
        return toSummary(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptDetailDto getAttemptFullDetail(Long attemptId, User requester) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Intento no encontrado"));
        ensureOwnership(attempt, requester);
        List<AttemptItemDto> items = attempt.getAnswers().stream()
                .map(this::toItem)
                .toList();
        return AttemptDetailDto.builder()
                .summary(toSummary(attempt))
                .items(items)
                .build();
    }

    private void ensureOwnership(Attempt attempt, User requester) {
        if (requester.getRole() == UserRole.STUDENT
                && !attempt.getUser().getId().equals(requester.getId())) {
            throw new RuntimeException("No autorizado para ver este intento");
        }
    }

    private List<Attempt> filterAttempts(User requester, boolean studentScope, String search, String status) {
        return attemptRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .filter(a -> !studentScope || a.getUser().getId().equals(requester.getId()))
                .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase("ALL")
                        || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> search == null || search.isBlank()
                        || a.getUser().getFullName().toLowerCase().contains(search.toLowerCase())
                        || a.getCaseStudy().getTitle().toLowerCase().contains(search.toLowerCase()))
                .toList();
    }

    private AttemptItemDto toItem(AttemptAnswer ans) {
        return AttemptItemDto.builder()
                .questionText(ans.getQuestion().getText())
                .selectedAnswer(ans.getSelectedOption().getText())
                .correct(ans.isCorrect())
                .category(ans.getSelectedOption().getCategory() != null
                        ? ans.getSelectedOption().getCategory().name() : "")
                .build();
    }

    private AttemptSummaryDto toSummary(Attempt a) {
        String date = a.getCompletedAt() != null
                ? a.getCompletedAt().format(FORMATTER)
                : a.getStartedAt().format(FORMATTER);

        return AttemptSummaryDto.builder()
                .id(a.getId())
                .studentName(a.getUser().getFullName())
                .studentEmail(a.getUser().getEmail())
                .caseTitle(a.getCaseStudy().getTitle())
                .attemptNumber(a.getAttemptNumber())
                .date(date)
                .totalScore(a.getTotalScore())
                .status(a.getStatus().name())
                .clinicalScore(a.getClinicalScore())
                .ethicalScore(a.getEthicalScore())
                .normativeScore(a.getNormativeScore())
                .build();
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
