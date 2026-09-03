package com.school.security.controllers.api;

import com.school.security.dtos.responses.ProjectReportResDto;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.ProjectReportReportService;
import com.school.security.services.contracts.ProjectReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/projects/{projectId}/report")
public class ProjectReportController {

    private final ProjectReportService projectReportService;
    private final ProjectReportReportService projectReportReportService;
    private final UserRepository userRepository;

    public ProjectReportController(ProjectReportService projectReportService,
                                   ProjectReportReportService projectReportReportService,
                                   UserRepository userRepository) {
        this.projectReportService = projectReportService;
        this.projectReportReportService = projectReportReportService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private RoleType resolvePrimaryRole(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> role.getName())
                .orElse(RoleType.USER);
    }

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY", "LAST_7_DAYS", "LAST_30_DAYS",
            "LAST_3_MONTHS", "THIS_YEAR", "CUSTOM"
    );

    @GetMapping
    public ProjectReportResDto getProjectReport(@PathVariable Long projectId) {
        Long currentUserId = getCurrentUserId();
        return projectReportService.getProjectReport(currentUserId, projectId);
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @PathVariable Long projectId,
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            return ResponseEntity.badRequest().build();
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) {
                    return ResponseEntity.badRequest().build();
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        User user = getCurrentUser();
        RoleType role = resolvePrimaryRole(user);
        byte[] pdfBytes = projectReportReportService.generateReport(
                currentUserId, projectId, role, period, startDate, endDate);

        String filename = "rapport-projet-" + projectId + "-" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}