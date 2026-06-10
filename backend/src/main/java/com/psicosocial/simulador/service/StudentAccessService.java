package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.StudentAccessCodeRepository;
import com.psicosocial.simulador.repository.UserRepository;
import com.psicosocial.simulador.security.CustomUserDetailsService;
import com.psicosocial.simulador.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class StudentAccessService {

    private static final int SESSION_MINUTES = 30;

    private final StudentAccessCodeRepository accessCodeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.access-code.expiration-minutes:10}")
    private int expirationMinutes;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, h:mm a", new Locale("es", "CO"));

    @Transactional
    public LoginStepResponse beginStudentAccess(User user) {
        if (!user.isEnabled()) {
            throw new RuntimeException("Tu cuenta está deshabilitada. Contacta al profesor.");
        }

        if (hasCompletedOnboarding(user)) {
            return LoginStepResponse.builder()
                    .step(LoginStep.DIRECT_ACCESS)
                    .message("Bienvenido de nuevo")
                    .email(user.getEmail())
                    .studentName(user.getFullName())
                    .auth(buildAuthResponse(user))
                    .build();
        }

        expireStaleCodes();

        Optional<StudentAccessCode> active = findActiveAccess(user);
        if (active.isPresent()) {
            StudentAccessCode record = refreshSession(active.get());
            return buildLoginStep(record, user);
        }

        StudentAccessCode request = StudentAccessCode.builder()
                .student(user)
                .fechaGeneracion(LocalDateTime.now())
                .estado(AccessCodeStatus.PENDIENTE)
                .utilizado(false)
                .build();
        refreshSession(request);
        accessCodeRepository.save(request);

        notifyAdmins(user);

        return LoginStepResponse.builder()
                .step(LoginStep.REQUEST_SUBMITTED)
                .email(user.getEmail())
                .studentName(user.getFullName())
                .accessSession(request.getSessionToken())
                .message("Paso 2 de 3: esperando al docente")
                .detail("Tu solicitud fue enviada. El profesor debe aprobarla desde «Solicitudes de acceso». " +
                        "Esta pantalla se actualizará sola cuando puedas ingresar tu código.")
                .build();
    }

    @Transactional
    public AuthResponse verifyAccessCode(VerifyAccessCodeRequest request) {
        StudentAccessCode record = resolveSession(request.getAccessSession(), request.getEmail().trim().toLowerCase());

        if (record.getEstado() != AccessCodeStatus.APROBADO) {
            throw new RuntimeException("Tu acceso aún no está aprobado o ya no está disponible.");
        }

        String code = request.getCode().trim();
        if (record.getCodigo() == null || !record.getCodigo().equals(code)) {
            throw new RuntimeException("Código incorrecto. Verifica los 6 dígitos e inténtalo de nuevo.");
        }

        if (isExpired(record)) {
            record.setEstado(AccessCodeStatus.EXPIRADO);
            accessCodeRepository.save(record);
            throw new RuntimeException("El código expiró. Vuelve a iniciar sesión para solicitar acceso nuevamente.");
        }

        record.setUtilizado(true);
        record.setEstado(AccessCodeStatus.UTILIZADO);
        record.setUsedAt(LocalDateTime.now());
        record.setSessionToken(null);
        record.setSessionExpiresAt(null);
        accessCodeRepository.save(record);

        return buildAuthResponse(record.getStudent());
    }

    @Transactional
    public AccessStatusResponse getAccessStatus(String accessSession, String email) {
        StudentAccessCode record = resolveSession(accessSession, email.trim().toLowerCase());
        User user = record.getStudent();

        expireStaleCodes();
        StudentAccessCode current = accessCodeRepository.findById(record.getId()).orElse(record);

        return buildStatusResponse(current, user);
    }

    @Transactional
    public List<AccessRequestSummaryDto> listAccessRequests(boolean pendingOnly) {
        expireStaleCodes();
        List<StudentAccessCode> records = pendingOnly
                ? accessCodeRepository.findByEstadoOrderByFechaGeneracionDesc(AccessCodeStatus.PENDIENTE)
                : accessCodeRepository.findAllByOrderByFechaGeneracionDesc();

        return records.stream()
                .sorted(Comparator.comparing(StudentAccessCode::getFechaGeneracion).reversed())
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional
    public ApproveAccessResponseDto approve(Long requestId, User admin) {
        StudentAccessCode record = accessCodeRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (record.getEstado() != AccessCodeStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden aprobar solicitudes pendientes");
        }

        String code = generateUniqueCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);

        record.setCodigo(code);
        record.setEstado(AccessCodeStatus.APROBADO);
        record.setFechaExpiracion(expiresAt);
        record.setApprovedBy(admin);
        record.setApprovedAt(now);
        refreshSession(record);
        accessCodeRepository.save(record);

        boolean emailSent = false;
        if (emailService.isConfigured()) {
            emailService.sendAccessCode(
                    record.getStudent().getEmail(),
                    record.getStudent().getFullName(),
                    code,
                    expirationMinutes
            );
            emailSent = true;
        }

        String studentMessage = emailSent
                ? "Tu acceso fue aprobado. Revisa tu correo (y spam) para obtener el código de 6 dígitos."
                : "Tu acceso fue aprobado. El docente te entregará el código de 6 dígitos para ingresar.";

        notificationService.notify(record.getStudent(), "Acceso aprobado", studentMessage,
                NotificationType.ACCESS_APPROVED, "/login");

        return ApproveAccessResponseDto.builder()
                .message("Acceso aprobado para " + record.getStudent().getFullName())
                .code(emailSent ? null : code)
                .expiresAt(expiresAt.format(FORMATTER))
                .emailSent(emailSent)
                .expiresInMinutes(expirationMinutes)
                .build();
    }

    @Transactional
    public MessageResponse reject(Long requestId, User admin) {
        StudentAccessCode record = accessCodeRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (record.getEstado() != AccessCodeStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden rechazar solicitudes pendientes");
        }

        record.setEstado(AccessCodeStatus.RECHAZADO);
        record.setApprovedBy(admin);
        record.setApprovedAt(LocalDateTime.now());
        record.setSessionToken(null);
        record.setSessionExpiresAt(null);
        accessCodeRepository.save(record);

        notificationService.notify(record.getStudent(), "Acceso rechazado",
                "El docente rechazó tu solicitud. Puedes volver a iniciar sesión para solicitar acceso nuevamente.",
                NotificationType.ACCESS_REJECTED, "/login");

        return MessageResponse.builder()
                .message("Solicitud rechazada")
                .build();
    }

    @Transactional
    public MessageResponse resetAllAccess() {
        long count = accessCodeRepository.count();
        accessCodeRepository.deleteAll();
        return MessageResponse.builder()
                .message("Se reiniciaron " + count + " solicitudes y códigos de acceso de estudiantes.")
                .build();
    }

    public AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private LoginStepResponse buildLoginStep(StudentAccessCode record, User user) {
        if (record.getEstado() == AccessCodeStatus.PENDIENTE) {
            return LoginStepResponse.builder()
                    .step(LoginStep.AWAITING_APPROVAL)
                    .email(user.getEmail())
                    .studentName(user.getFullName())
                    .accessSession(record.getSessionToken())
                    .message("Paso 2 de 3: esperando al docente")
                    .detail("Ya tienes una solicitud pendiente. El profesor debe aprobarla desde «Solicitudes de acceso».")
                    .build();
        }

        if (record.getEstado() == AccessCodeStatus.APROBADO && !record.isUtilizado() && !isExpired(record)) {
            boolean emailConfigured = emailService.isConfigured();
            return LoginStepResponse.builder()
                    .step(LoginStep.CODE_REQUIRED)
                    .email(user.getEmail())
                    .studentName(user.getFullName())
                    .accessSession(record.getSessionToken())
                    .expiresInMinutes(remainingMinutes(record))
                    .expiresAt(formatExpiry(record))
                    .emailSent(emailConfigured)
                    .message("Paso 3 de 3: ingresa tu código")
                    .detail(emailConfigured
                            ? "Revisa tu correo institucional. El código tiene 6 dígitos y vence en "
                            + remainingMinutes(record) + " min."
                            : "El docente te entregó un código de 6 dígitos. Escríbelo aquí para entrar.")
                    .build();
        }

        throw new RuntimeException("No hay un acceso activo. Intenta iniciar sesión nuevamente.");
    }

    private AccessStatusResponse buildStatusResponse(StudentAccessCode record, User user) {
        if (record.getEstado() == AccessCodeStatus.PENDIENTE) {
            return AccessStatusResponse.builder()
                    .status(AccessCodeStatus.PENDIENTE)
                    .studentName(user.getFullName())
                    .message("Esperando aprobación del docente")
                    .detail("Tu solicitud sigue pendiente. No cierres esta ventana; se actualizará automáticamente.")
                    .canEnterCode(false)
                    .canRetry(false)
                    .build();
        }

        if (record.getEstado() == AccessCodeStatus.APROBADO && !record.isUtilizado() && !isExpired(record)) {
            boolean emailConfigured = emailService.isConfigured();
            return AccessStatusResponse.builder()
                    .status(AccessCodeStatus.APROBADO)
                    .studentName(user.getFullName())
                    .message("¡Acceso aprobado!")
                    .detail(emailConfigured
                            ? "Revisa tu correo e ingresa el código de 6 dígitos abajo."
                            : "Pide el código de 6 dígitos a tu docente e ingrésalo abajo.")
                    .canEnterCode(true)
                    .canRetry(false)
                    .expiresInMinutes(remainingMinutes(record))
                    .expiresAt(formatExpiry(record))
                    .build();
        }

        if (record.getEstado() == AccessCodeStatus.RECHAZADO) {
            return AccessStatusResponse.builder()
                    .status(AccessCodeStatus.RECHAZADO)
                    .studentName(user.getFullName())
                    .message("Solicitud rechazada")
                    .detail("Contacta a tu docente o vuelve a iniciar sesión para enviar una nueva solicitud.")
                    .canEnterCode(false)
                    .canRetry(true)
                    .build();
        }

        return AccessStatusResponse.builder()
                .status(AccessCodeStatus.EXPIRADO)
                .studentName(user.getFullName())
                .message("Código expirado")
                .detail("Vuelve a iniciar sesión con tu correo y contraseña para solicitar acceso otra vez.")
                .canEnterCode(false)
                .canRetry(true)
                .build();
    }

    private boolean hasCompletedOnboarding(User user) {
        return accessCodeRepository.existsByStudentAndEstado(user, AccessCodeStatus.UTILIZADO);
    }

    private Optional<StudentAccessCode> findActiveAccess(User user) {
        Optional<StudentAccessCode> pending = accessCodeRepository
                .findFirstByStudentAndEstadoOrderByFechaGeneracionDesc(user, AccessCodeStatus.PENDIENTE);
        if (pending.isPresent()) {
            return pending;
        }

        return accessCodeRepository
                .findFirstByStudentAndEstadoInAndUtilizadoFalseOrderByFechaGeneracionDesc(
                        user, List.of(AccessCodeStatus.APROBADO))
                .filter(r -> !isExpired(r));
    }

    private StudentAccessCode resolveSession(String accessSession, String email) {
        if (accessSession == null || accessSession.isBlank()) {
            throw new RuntimeException("Sesión de acceso inválida. Vuelve a iniciar sesión.");
        }

        StudentAccessCode record = accessCodeRepository.findBySessionToken(accessSession.trim())
                .orElseThrow(() -> new RuntimeException("Sesión expirada. Vuelve a iniciar sesión con tu correo y contraseña."));

        if (!record.getStudent().getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("La sesión no corresponde a este correo.");
        }

        if (isSessionExpired(record)) {
            throw new RuntimeException("Tu sesión expiró. Vuelve a iniciar sesión con tu correo y contraseña.");
        }

        return record;
    }

    private StudentAccessCode refreshSession(StudentAccessCode record) {
        record.setSessionToken(UUID.randomUUID().toString().replace("-", ""));
        record.setSessionExpiresAt(LocalDateTime.now().plusMinutes(SESSION_MINUTES));
        return accessCodeRepository.save(record);
    }

    private void notifyAdmins(User student) {
        userRepository.findByRole(UserRole.ADMIN).forEach(admin ->
                notificationService.notify(admin, "Solicitud de acceso",
                        student.getFullName() + " (" + student.getEmail() + ") solicita ingresar al simulador.",
                        NotificationType.INFO, "/access-requests")
        );
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            if (!accessCodeRepository.existsByCodigo(code)) {
                return code;
            }
        }
        throw new RuntimeException("No se pudo generar un código único. Intenta de nuevo.");
    }

    private void expireStaleCodes() {
        accessCodeRepository.findByEstadoOrderByFechaGeneracionDesc(AccessCodeStatus.APROBADO).stream()
                .filter(this::isExpired)
                .filter(r -> !r.isUtilizado())
                .forEach(record -> {
                    record.setEstado(AccessCodeStatus.EXPIRADO);
                    record.setSessionToken(null);
                    record.setSessionExpiresAt(null);
                    accessCodeRepository.save(record);
                });
    }

    private boolean isExpired(StudentAccessCode record) {
        return record.getFechaExpiracion() != null
                && record.getFechaExpiracion().isBefore(LocalDateTime.now());
    }

    private boolean isSessionExpired(StudentAccessCode record) {
        return record.getSessionExpiresAt() == null
                || record.getSessionExpiresAt().isBefore(LocalDateTime.now());
    }

    private int remainingMinutes(StudentAccessCode record) {
        if (record.getFechaExpiracion() == null) return expirationMinutes;
        long minutes = java.time.Duration.between(LocalDateTime.now(), record.getFechaExpiracion()).toMinutes();
        return (int) Math.max(1, minutes);
    }

    private String formatExpiry(StudentAccessCode record) {
        return record.getFechaExpiracion() != null ? record.getFechaExpiracion().format(FORMATTER) : null;
    }

    private AccessRequestSummaryDto toSummaryDto(StudentAccessCode record) {
        return AccessRequestSummaryDto.builder()
                .id(record.getId())
                .studentId(record.getStudent().getId())
                .studentName(record.getStudent().getFullName())
                .studentEmail(record.getStudent().getEmail())
                .studentAvatar(record.getStudent().getAvatarUrl())
                .status(record.getEstado())
                .requestedAt(record.getFechaGeneracion().format(FORMATTER))
                .timeAgo(NotificationService.timeAgo(record.getFechaGeneracion()))
                .approvedByName(record.getApprovedBy() != null ? record.getApprovedBy().getFullName() : null)
                .approvedAt(record.getApprovedAt() != null ? record.getApprovedAt().format(FORMATTER) : null)
                .expiresAt(record.getFechaExpiracion() != null ? record.getFechaExpiracion().format(FORMATTER) : null)
                .build();
    }
}
