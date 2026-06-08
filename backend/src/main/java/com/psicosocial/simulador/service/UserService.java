package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;
    private final CaseStudyRepository caseStudyRepository;
    private final ResetRequestRepository resetRequestRepository;
    private final StudentGroupRepository groupRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm a", new Locale("es", "CO"));

    @Transactional(readOnly = true)
    public List<UserDto> listUsers(String search, String role, String status) {
        return userRepository.findAll().stream()
                .filter(u -> search == null || search.isBlank()
                        || u.getFullName().toLowerCase().contains(search.toLowerCase())
                        || u.getEmail().toLowerCase().contains(search.toLowerCase()))
                .filter(u -> role == null || role.isBlank() || role.equalsIgnoreCase("ALL")
                        || u.getRole().name().equalsIgnoreCase(role))
                .filter(u -> matchesStatus(u, status))
                .sorted(Comparator.comparing(User::getId))
                .map(this::toDto)
                .toList();
    }

    private boolean matchesStatus(User u, String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return true;
        return switch (status.toUpperCase()) {
            case "ENABLED" -> u.isEnabled();
            case "DISABLED" -> !u.isEnabled();
            case "BLOCKED" -> isBlocked(u);
            default -> true;
        };
    }

    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<Attempt> attempts = attemptRepository.findByUserOrderByStartedAtDesc(user);
        double avgC = attempts.stream().filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .mapToDouble(Attempt::getClinicalScore).average().orElse(0);
        double avgE = attempts.stream().filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .mapToDouble(Attempt::getEthicalScore).average().orElse(0);
        double avgN = attempts.stream().filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .mapToDouble(Attempt::getNormativeScore).average().orElse(0);

        return UserDetailDto.builder()
                .user(toDto(user))
                .attempts(attempts.stream().map(this::toAttemptSummary).toList())
                .averageClinical(round(avgC))
                .averageEthical(round(avgE))
                .averageNormative(round(avgN))
                .build();
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Ya existe un usuario con ese correo");
        }
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole() != null ? req.getRole() : UserRole.STUDENT)
                .avatarUrl(resolveAvatarUrl(req.getAvatarUrl()))
                .maxAttempts(req.getMaxAttempts() != null ? req.getMaxAttempts() : 3)
                .enabled(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);
        notificationService.notify(user, "Bienvenido a Misión Psicosocial",
                "Tu cuenta ha sido creada. Ya puedes iniciar los casos asignados.",
                NotificationType.ACCOUNT, "/dashboard");
        return toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail() != null) {
            String email = req.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndIdNot(email, id)) {
                throw new RuntimeException("Ya existe un usuario con ese correo");
            }
            user.setEmail(email);
        }
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(resolveAvatarUrl(req.getAvatarUrl()));
        }
        if (req.getMaxAttempts() != null) user.setMaxAttempts(req.getMaxAttempts());
        if (req.getEnabled() != null) user.setEnabled(req.getEnabled());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public MessageResponse deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        groupRepository.findAllByMemberId(user.getId()).forEach(group -> {
            group.getMembers().remove(user);
            groupRepository.save(group);
        });
        attemptRepository.findByUserOrderByStartedAtDesc(user).forEach(attemptRepository::delete);
        userRepository.delete(user);
        return MessageResponse.builder().message("Usuario eliminado").build();
    }

    @Transactional
    public UserDto setEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setEnabled(enabled);
        userRepository.save(user);
        notificationService.notify(user,
                enabled ? "Cuenta habilitada" : "Cuenta deshabilitada",
                enabled ? "Tu cuenta ha sido habilitada por el profesor." : "Tu cuenta ha sido deshabilitada temporalmente.",
                enabled ? NotificationType.SUCCESS : NotificationType.WARNING, null);
        return toDto(user);
    }

    @Transactional
    public UserDto changeRole(Long id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setRole(role);
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public UserDto setMaxAttempts(Long id, int maxAttempts) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setMaxAttempts(Math.max(1, maxAttempts));
        userRepository.save(user);
        return toDto(user);
    }

    @Transactional
    public UserDto resetAttempts(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        attemptRepository.findByUserOrderByStartedAtDesc(user).forEach(a -> {
            a.setArchived(true);
            attemptRepository.save(a);
        });
        resetRequestRepository.findByApprovedFalse().stream()
                .filter(r -> r.getUser().getId().equals(id))
                .forEach(r -> {
                    r.setApproved(true);
                    resetRequestRepository.save(r);
                });
        notificationService.notify(user, "Intentos reiniciados",
                "El profesor ha reiniciado todos tus intentos. Ya puedes volver a iniciar los casos.",
                NotificationType.RESET_APPROVED, "/dashboard");
        return toDto(user);
    }

    private boolean isBlocked(User user) {
        return userRepository.findById(user.getId())
                .map(u -> attemptRepository.findByUserAndArchivedFalseOrderByStartedAtDesc(u).stream()
                        .anyMatch(a -> a.getStatus() == AttemptStatus.BLOCKED))
                .orElse(false);
    }

    private UserDto toDto(User user) {
        List<Attempt> attempts = attemptRepository.findByUserOrderByStartedAtDesc(user);
        long passed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.PASSED).count();
        double avg = attempts.stream().filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .mapToDouble(Attempt::getTotalScore).average().orElse(0);
        boolean blocked = attemptRepository.findByUserAndArchivedFalseOrderByStartedAtDesc(user).stream()
                .anyMatch(a -> a.getStatus() == AttemptStatus.BLOCKED);

        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .enabled(user.isEnabled())
                .maxAttempts(user.getMaxAttempts())
                .blocked(blocked)
                .totalAttempts(attempts.size())
                .passedAttempts(passed)
                .averageScore(round(avg))
                .authProvider(user.getAuthProvider())
                .build();
    }

    private AttemptSummaryDto toAttemptSummary(Attempt a) {
        String date = a.getCompletedAt() != null ? a.getCompletedAt().format(FORMATTER) : a.getStartedAt().format(FORMATTER);
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

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private String resolveAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) return null;
        String url = avatarUrl.trim();
        return url.isBlank() ? null : url;
    }
}
