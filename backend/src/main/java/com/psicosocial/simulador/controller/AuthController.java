package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginStepResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-access-code")
    public ResponseEntity<AuthResponse> verifyAccessCode(@Valid @RequestBody VerifyAccessCodeRequest request) {
        return ResponseEntity.ok(authService.verifyAccessCode(request));
    }

    @GetMapping("/access-status")
    public ResponseEntity<AccessStatusResponse> accessStatus(
            @RequestParam String accessSession,
            @RequestParam String email
    ) {
        return ResponseEntity.ok(authService.getAccessStatus(accessSession, email));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<LoginStepResponse> oauthGoogle(@Valid @RequestBody OAuthGoogleRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/oauth/facebook")
    public ResponseEntity<LoginStepResponse> oauthFacebook(@Valid @RequestBody OAuthFacebookRequest request) {
        return ResponseEntity.ok(authService.loginWithFacebook(request));
    }
}
