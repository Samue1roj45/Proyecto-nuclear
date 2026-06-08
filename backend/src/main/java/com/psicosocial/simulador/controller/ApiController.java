package com.psicosocial.simulador.controller;

import com.psicosocial.simulador.dto.*;
import com.psicosocial.simulador.model.User;
import com.psicosocial.simulador.security.CustomUserDetailsService;
import com.psicosocial.simulador.service.DashboardService;
import com.psicosocial.simulador.service.ReportsService;
import com.psicosocial.simulador.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class ApiController {

    private final CustomUserDetailsService userDetailsService;
    private final DashboardService dashboardService;
    private final SimulationService simulationService;
    private final ReportsService reportsService;

    private User currentUser(Authentication auth) {
        return userDetailsService.getUser(auth.getName());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StudentDashboardDto> dashboard(
            Authentication auth,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(dashboardService.getStudentDashboard(currentUser(auth), search));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<CaseDetailDto> getCase(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(simulationService.getCaseDetail(id, currentUser(auth)));
    }

    @PostMapping("/cases/{id}/start")
    public ResponseEntity<CaseDetailDto> startCase(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(simulationService.startAttempt(id, currentUser(auth)));
    }

    @PostMapping("/cases/{id}/answer")
    public ResponseEntity<CaseDetailDto> submitAnswer(
            @PathVariable Long id,
            Authentication auth,
            @RequestBody SubmitAnswerRequest request
    ) {
        return ResponseEntity.ok(simulationService.submitAnswer(id, currentUser(auth), request));
    }

    @PostMapping("/cases/{id}/reset-request")
    public ResponseEntity<MessageResponse> requestReset(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(simulationService.requestReset(id, currentUser(auth)));
    }

    @GetMapping("/reports")
    public ResponseEntity<ReportsSummaryDto> reports(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(reportsService.getReports(currentUser(auth), search, status, sort));
    }

    @GetMapping("/reports/attempts/{id}")
    public ResponseEntity<AttemptSummaryDto> attemptDetail(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reportsService.getAttemptDetail(id, currentUser(auth)));
    }

    @GetMapping("/reports/attempts/{id}/full")
    public ResponseEntity<AttemptDetailDto> attemptFullDetail(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(reportsService.getAttemptFullDetail(id, currentUser(auth)));
    }

    @GetMapping(value = "/reports/export", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        String csv = reportsService.exportCsv(currentUser(auth), search, status);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reportes.csv")
                .body(csv);
    }
}
