package com.school.security.controllers.api;

import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.entities.User;
import com.school.security.exceptions.ResourceNotFoundException;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardReportService dashboardReportService;
    private final UserRepository userRepository;

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY",
            "LAST_7_DAYS",
            "LAST_30_DAYS",
            "LAST_3_MONTHS",
            "THIS_YEAR",
            "CUSTOM");

    public DashboardController(
            DashboardService dashboardService,
            DashboardReportService dashboardReportService,
            UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.dashboardReportService = dashboardReportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardDataResDto> getDashboardStats(
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
            LocalDate start;
            LocalDate end;
            try {
                start = LocalDate.parse(startDate);
                end = LocalDate.parse(endDate);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        DashboardDataResDto result =
                dashboardService.getDashboardStats(currentUserId, period, startDate, endDate);
        return ResponseEntity.ok(result);
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
            LocalDate start;
            LocalDate end;
            try {
                start = LocalDate.parse(startDate);
                end = LocalDate.parse(endDate);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        byte[] pdfBytes =
                dashboardReportService.generateReport(currentUserId, period, startDate, endDate);

        String filename =
                "rapport-statistiques-"
                        + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getUsersId();
    }
}
