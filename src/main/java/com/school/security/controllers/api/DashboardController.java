package com.school.security.controllers.api;

import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.entities.User;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.DashboardService;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private static final Set<String> VALID_PERIODS = Set.of(
            "TODAY",
            "LAST_7_DAYS",
            "LAST_30_DAYS",
            "LAST_3_MONTHS",
            "THIS_YEAR",
            "CUSTOM");

    public DashboardController(DashboardService dashboardService, UserRepository userRepository) {
        this.dashboardService = dashboardService;
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
            java.time.LocalDate start;
            java.time.LocalDate end;
            try {
                start = java.time.LocalDate.parse(startDate);
                end = java.time.LocalDate.parse(endDate);
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().build();
            }
        }

        Long currentUserId = getCurrentUserId();
        DashboardDataResDto result = dashboardService.getDashboardStats(currentUserId, period, startDate, endDate);
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }
}
