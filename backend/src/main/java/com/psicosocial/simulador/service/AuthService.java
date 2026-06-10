package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.*;
import com.psicosocial.simulador.repository.PasswordResetTokenRepository;
import com.psicosocial.simulador.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final OAuthService oauthService;
    private final StudentAccessService studentAccessService;

    private static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos";

    public LoginStepResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Esta cuenta usa inicio con "
                    + providerLabel(user.getAuthProvider()) + ". Usa ese método para entrar.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new RuntimeException(INVALID_CREDENTIALS);
        }

        user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(INVALID_CREDENTIALS));

        if (user.getRole() == UserRole.ADMIN) {
            return LoginStepResponse.builder()
                    .step(LoginStep.DIRECT_ACCESS)
                    .message("Acceso concedido")
                    .email(user.getEmail())
                    .auth(studentAccessService.buildAuthResponse(user))
                    .build();
        }

        return studentAccessService.beginStudentAccess(user);
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Ya existe una cuenta con ese correo");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STUDENT)
                .enabled(true)
                .maxAttempts(3)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        notificationService.notify(user, "Cuenta creada",
                "Tu registro fue exitoso. Al iniciar sesión, tu docente debe aprobar tu acceso y recibirás un código temporal.",
                NotificationType.ACCOUNT, "/login");

        return MessageResponse.builder()
                .message("Cuenta creada. Al iniciar sesión, tu docente aprobará tu acceso y te dará un código de 6 dígitos.")
                .build();
    }

    @Transactional
    public LoginStepResponse loginWithGoogle(OAuthGoogleRequest request) {
        OAuthService.OAuthProfile profile = oauthService.verifyGoogleCode(request.getCode());
        return oauthLogin(AuthProvider.GOOGLE, profile);
    }

    @Transactional
    public LoginStepResponse loginWithFacebook(OAuthFacebookRequest request) {
        OAuthService.OAuthProfile profile = oauthService.verifyFacebookToken(request.getAccessToken());
        return oauthLogin(AuthProvider.FACEBOOK, profile);
    }

    public AuthResponse verifyAccessCode(VerifyAccessCodeRequest request) {
        return studentAccessService.verifyAccessCode(request);
    }

    public AccessStatusResponse getAccessStatus(String accessSession, String email) {
        return studentAccessService.getAccessStatus(accessSession, email);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No encontramos una cuenta con ese correo"));

        passwordResetTokenRepository.deleteByUser(user);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(code)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetCode(user.getEmail(), user.getFullName(), code);

        return ForgotPasswordResponse.builder()
                .message("Te enviamos un código de recuperación a " + maskEmail(user.getEmail())
                        + ". Revisa tu bandeja de entrada y spam.")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Correo no válido"));

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenAndUsedFalse(request.getResetCode().trim())
                .orElseThrow(() -> new RuntimeException("Código de recuperación inválido o ya usado"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("El código no corresponde a este correo");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código de recuperación ha expirado");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            user.setAuthProvider(AuthProvider.LOCAL);
            user.setProviderId(null);
        }
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        notificationService.notify(user, "Contraseña actualizada",
                "Tu contraseña fue restablecida correctamente.",
                NotificationType.SUCCESS, "/login");

        return MessageResponse.builder()
                .message("Contraseña actualizada. Ya puedes iniciar sesión.")
                .build();
    }

    private LoginStepResponse oauthLogin(AuthProvider provider, OAuthService.OAuthProfile profile) {
        User user = userRepository.findByAuthProviderAndProviderId(provider, profile.providerId())
                .orElseGet(() -> userRepository.findByEmail(profile.email())
                        .map(existing -> linkOAuthAccount(existing, provider, profile))
                        .orElseGet(() -> createOAuthUser(provider, profile)));

        if (!user.isEnabled()) {
            throw new RuntimeException("Tu cuenta está deshabilitada. Contacta al profesor.");
        }

        if (user.getRole() == UserRole.ADMIN) {
            return LoginStepResponse.builder()
                    .step(LoginStep.DIRECT_ACCESS)
                    .message("Acceso concedido")
                    .email(user.getEmail())
                    .auth(studentAccessService.buildAuthResponse(user))
                    .build();
        }

        return studentAccessService.beginStudentAccess(user);
    }

    private User createOAuthUser(AuthProvider provider, OAuthService.OAuthProfile profile) {
        User user = User.builder()
                .fullName(profile.fullName() != null && !profile.fullName().isBlank()
                        ? profile.fullName() : profile.email())
                .email(profile.email())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(UserRole.STUDENT)
                .avatarUrl(resolveOAuthAvatar(profile.avatarUrl()))
                .enabled(true)
                .maxAttempts(3)
                .authProvider(provider)
                .providerId(profile.providerId())
                .build();
        userRepository.save(user);

        notificationService.notify(user, "Bienvenido a Misión Psicosocial",
                "Tu cuenta con " + providerLabel(provider) + " fue creada correctamente.",
                NotificationType.ACCOUNT, "/dashboard");
        return user;
    }

    private User linkOAuthAccount(User user, AuthProvider provider, OAuthService.OAuthProfile profile) {
        user.setAuthProvider(provider);
        user.setProviderId(profile.providerId());
        if (profile.fullName() != null && !profile.fullName().isBlank()) {
            user.setFullName(profile.fullName());
        }
        if (!hasCustomAvatar(user.getAvatarUrl()) && hasCustomAvatar(profile.avatarUrl())) {
            user.setAvatarUrl(profile.avatarUrl().trim());
        }
        userRepository.save(user);
        return user;
    }

    private String resolveOAuthAvatar(String avatarUrl) {
        return hasCustomAvatar(avatarUrl) ? avatarUrl.trim() : null;
    }

    private boolean hasCustomAvatar(String avatarUrl) {
        return avatarUrl != null && !avatarUrl.isBlank();
    }

    private String providerLabel(AuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> "Google";
            case FACEBOOK -> "Facebook";
            default -> "correo y contraseña";
        };
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }
}
