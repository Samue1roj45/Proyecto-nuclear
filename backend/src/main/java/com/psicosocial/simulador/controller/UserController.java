package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.UserRole;
import com.psicosocial.simulador.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(userService.listUsers(search, role, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserDto> enable(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setEnabled(id, true));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserDto> disable(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setEnabled(id, false));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserDto> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.changeRole(id, UserRole.valueOf(body.get("role"))));
    }

    @PatchMapping("/{id}/max-attempts")
    public ResponseEntity<UserDto> maxAttempts(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(userService.setMaxAttempts(id, body.get("maxAttempts")));
    }

    @PostMapping("/{id}/reset-attempts")
    public ResponseEntity<UserDto> resetAttempts(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resetAttempts(id));
    }
}
