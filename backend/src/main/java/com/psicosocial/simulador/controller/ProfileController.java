package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.security.CustomUserDetailsService;
import com.psicosocial.simulador.service.ProfileService;
import com.psicosocial.simulador.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin
public class ProfileController {

    private final ProfileService profileService;
    private final StorageService storageService;
    private final CustomUserDetailsService userDetailsService;

    private User current(Authentication auth) {
        return userDetailsService.getUser(auth.getName());
    }

    @GetMapping
    public ResponseEntity<UserDto> me(Authentication auth) {
        User u = current(auth);
        return ResponseEntity.ok(UserDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole())
                .avatarUrl(u.getAvatarUrl())
                .enabled(u.isEnabled())
                .maxAttempts(u.getMaxAttempts())
                .authProvider(u.getAuthProvider())
                .build());
    }

    @PutMapping
    public ResponseEntity<UserDto> update(Authentication auth, @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(profileService.updateProfile(current(auth), req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(profileService.changePassword(current(auth), req));
    }

    @PostMapping("/avatar")
    public ResponseEntity<UserDto> uploadAvatar(Authentication auth, @RequestParam("file") MultipartFile file) {
        String url = storageService.storeAvatar(file);
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setAvatarUrl(url);
        return ResponseEntity.ok(profileService.updateProfile(current(auth), req));
    }
}
