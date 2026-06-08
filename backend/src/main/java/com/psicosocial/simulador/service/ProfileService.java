package com.psicosocial.simulador.service;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.AuthProvider;
import com.psicosocial.simulador.model.NotificationType;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public UserDto updateProfile(User user, UpdateProfileRequest req) {
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getAvatarUrl() != null) {
            String url = req.getAvatarUrl().trim();
            user.setAvatarUrl(url.isBlank() ? null : url);
        }
        userRepository.save(user);
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .enabled(user.isEnabled())
                .maxAttempts(user.getMaxAttempts())
                .authProvider(user.getAuthProvider())
                .build();
    }

    @Transactional
    public MessageResponse changePassword(User user, ChangePasswordRequest req) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException("Tu cuenta usa inicio de sesión con red social. No puedes cambiar la contraseña aquí.");
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        if (req.getNewPassword().length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        notificationService.notify(user, "Contraseña actualizada",
                "Tu contraseña se cambió correctamente.", NotificationType.SUCCESS, null);
        return MessageResponse.builder().message("Contraseña actualizada correctamente").build();
    }
}
