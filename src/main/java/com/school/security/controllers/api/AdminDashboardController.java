package com.school.security.controllers.api;

import com.school.security.dtos.responses.AdminDashboardStatsResDto;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.AdminDashboardReportService;
import com.school.security.services.contracts.AdminDashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AdminDashboardReportService adminDashboardReportService;
    private final UserRepository userRepository;

    public AdminDashboardController(AdminDashboardService adminDashboardService,
                                    AdminDashboardReportService adminDashboardReportService,
                                    UserRepository userRepository) {
        this.adminDashboardService = adminDashboardService;
        this.adminDashboardReportService = adminDashboardReportService;
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

    @GetMapping("/stats")
    public AdminDashboardStatsResDto getAdminDashboardStats(
            @RequestParam String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (period == null || period.isBlank() || !VALID_PERIODS.contains(period)) {
            throw new IllegalArgumentException("Période invalide");
        }

        if ("CUSTOM".equals(period)) {
            if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                throw new IllegalArgumentException("Dates requises pour période personnalisée");
            }
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (start.isAfter(end)) throw new IllegalArgumentException("Date début après date fin");
            } catch (Exception e) {
                throw new IllegalArgumentException("Format de date invalide");
            }
        }

        Long currentUserId = getCurrentUserId();
        return adminDashboardService.getAdminDashboardStats(currentUserId, period, startDate, endDate);
    }

    @GetMapping("/reports/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
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
        byte[] pdfBytes = adminDashboardReportService.generateReport(
                currentUserId, role, period, startDate, endDate);

        String filename = "rapport-admin-" + LocalDate.now() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}