package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.service.AdminService;
import com.psicosocial.simulador.service.CaseAdminService;
import com.psicosocial.simulador.service.GroupService;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.security.CustomUserDetailsService;
import com.psicosocial.simulador.service.StudentAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CaseAdminService caseAdminService;
    private final GroupService groupService;
    private final StudentAccessService studentAccessService;
    private final CustomUserDetailsService userDetailsService;

    private User current(Authentication auth) {
        return userDetailsService.getUser(auth.getName());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> stats() {
        return ResponseEntity.ok(adminService.getAdminStats());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> leaderboard() {
        return ResponseEntity.ok(adminService.buildLeaderboard());
    }

    @GetMapping("/reset-requests")
    public ResponseEntity<List<ResetRequestSummaryDto>> resetRequests(
            @RequestParam(required = false, defaultValue = "true") boolean pendingOnly
    ) {
        return ResponseEntity.ok(adminService.listResetRequests(pendingOnly));
    }

    @PostMapping("/reset-requests/{id}/approve")
    public ResponseEntity<MessageResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveReset(id));
    }

    @PostMapping("/reset-requests/{id}/reject")
    public ResponseEntity<MessageResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectReset(id));
    }

    @GetMapping("/access-requests")
    public ResponseEntity<List<AccessRequestSummaryDto>> accessRequests(
            @RequestParam(required = false, defaultValue = "true") boolean pendingOnly
    ) {
        return ResponseEntity.ok(studentAccessService.listAccessRequests(pendingOnly));
    }

    @PostMapping("/access-requests/reset-all")
    public ResponseEntity<MessageResponse> resetAllAccess() {
        return ResponseEntity.ok(studentAccessService.resetAllAccess());
    }

    @PostMapping("/access-requests/{id}/approve")
    public ResponseEntity<ApproveAccessResponseDto> approveAccess(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(studentAccessService.approve(id, current(auth)));
    }

    @PostMapping("/access-requests/{id}/reject")
    public ResponseEntity<MessageResponse> rejectAccess(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(studentAccessService.reject(id, current(auth)));
    }

    @GetMapping("/cases")
    public ResponseEntity<List<CaseAdminDto>> listCases(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(caseAdminService.listCases(search));
    }

    @PostMapping("/cases")
    public ResponseEntity<CaseAdminDto> createCase(@RequestBody CaseRequest req) {
        return ResponseEntity.ok(caseAdminService.createCase(req));
    }

    @PutMapping("/cases/{id}")
    public ResponseEntity<CaseAdminDto> updateCase(@PathVariable Long id, @RequestBody CaseRequest req) {
        return ResponseEntity.ok(caseAdminService.updateCase(id, req));
    }

    @DeleteMapping("/cases/{id}")
    public ResponseEntity<MessageResponse> deleteCase(@PathVariable Long id) {
        return ResponseEntity.ok(caseAdminService.deleteCase(id));
    }

    @GetMapping("/cases/{id}/questions")
    public ResponseEntity<List<QuestionAdminDto>> listQuestions(@PathVariable Long id) {
        return ResponseEntity.ok(caseAdminService.listQuestions(id));
    }

    @PostMapping("/cases/{id}/questions")
    public ResponseEntity<MessageResponse> addQuestion(@PathVariable Long id, @RequestBody QuestionRequest req) {
        caseAdminService.addQuestion(id, req);
        return ResponseEntity.ok(MessageResponse.builder().message("Pregunta agregada").build());
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<QuestionAdminDto> updateQuestion(@PathVariable Long id, @RequestBody QuestionRequest req) {
        return ResponseEntity.ok(caseAdminService.updateQuestion(id, req));
    }

    @PutMapping("/cases/{id}/questions/reorder")
    public ResponseEntity<MessageResponse> reorderQuestions(@PathVariable Long id, @RequestBody ReorderQuestionsRequest req) {
        return ResponseEntity.ok(caseAdminService.reorderQuestions(id, req));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<MessageResponse> deleteQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(caseAdminService.deleteQuestion(id));
    }

    // ===== Grupos de estudiantes =====

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDto>> listGroups() {
        return ResponseEntity.ok(groupService.listGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupDto> groupDetail(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroup(id));
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest req) {
        return ResponseEntity.ok(groupService.createGroup(req));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<GroupDto> updateGroup(@PathVariable Long id, @RequestBody UpdateGroupRequest req) {
        return ResponseEntity.ok(groupService.updateGroup(id, req));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<MessageResponse> deleteGroup(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.deleteGroup(id));
    }
}
