package com.school.security.controllers.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.school.security.dtos.responses.*;
import com.school.security.entities.Direction;
import com.school.security.entities.User;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock private DashboardService dashboardService;
    @Mock private DashboardReportService dashboardReportService;
    @Mock private UserRepository userRepository;

    @InjectMocks private DashboardController dashboardController;

    private DashboardDataResDto sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        sampleResponse = buildSampleResponse();
        authenticateUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── Stats endpoint tests ─────────────────────────────────────

    @Test
    void getDashboardStatsWithTODAYPeriodShouldReturn200() throws Exception {
        mockService("TODAY", null, null);

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "TODAY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"))
                .andExpect(jsonPath("$.stats.projects").value(2))
                .andExpect(jsonPath("$.stats.tasks").value(15))
                .andExpect(jsonPath("$.stats.totalUsers").value(42))
                .andExpect(jsonPath("$.evolution.points").isArray())
                .andExpect(jsonPath("$.distribution.items").isArray())
                .andExpect(jsonPath("$.distribution.total").value(15));
    }

    @Test
    void getDashboardStatsWithLAST7DAYSPeriodShouldReturn200() throws Exception {
        mockService("LAST_7_DAYS", null, null);

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "LAST_7_DAYS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"));
    }

    @Test
    void getDashboardStatsWithLAST30DAYSPeriodShouldReturn200() throws Exception {
        mockService("LAST_30_DAYS", null, null);

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "LAST_30_DAYS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"));
    }

    @Test
    void getDashboardStatsWithLAST3MONTHSPeriodShouldReturn200() throws Exception {
        mockService("LAST_3_MONTHS", null, null);

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "LAST_3_MONTHS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"));
    }

    @Test
    void getDashboardStatsWithTHISYEARPeriodShouldReturn200() throws Exception {
        mockService("THIS_YEAR", null, null);

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "THIS_YEAR")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"));
    }

    @Test
    void getDashboardStatsWithCUSTOMPeriodAndValidDatesShouldReturn200() throws Exception {
        mockService("CUSTOM", "2026-08-01", "2026-08-25");

        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "CUSTOM")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-25")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("TODAY"));
    }

    @Test
    void getDashboardStatsWithInvalidPeriodShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "INVALID_PERIOD")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboardStatsWithCUSTOMPeriodAndMissingDatesShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "CUSTOM")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboardStatsWithCUSTOMPeriodAndInvertedDatesShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "CUSTOM")
                        .param("startDate", "2026-08-25")
                        .param("endDate", "2026-08-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboardStatsWithBlankPeriodShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/stats")
                        .param("period", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ─── PDF report endpoint tests ────────────────────────────────

    @Test
    void generatePdfReportWithValidPeriodShouldReturn200AndPdfContent() throws Exception {
        mockUserRepository();
        byte[] fakePdf = "%PDF-1.4 fake content".getBytes();
        when(dashboardReportService.generateReport(eq(1L), eq("TODAY"), isNull(), isNull()))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/dashboard/reports/pdf")
                        .param("period", "TODAY")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    void generatePdfReportWithInvalidPeriodShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/reports/pdf")
                        .param("period", "INVALID")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePdfReportWithCUSTOMPeriodMissingDatesShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/reports/pdf")
                        .param("period", "CUSTOM")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePdfReportWithCUSTOMPeriodInvertedDatesShouldReturn400() throws Exception {
        mockMvc.perform(get("/dashboard/reports/pdf")
                        .param("period", "CUSTOM")
                        .param("startDate", "2026-08-25")
                        .param("endDate", "2026-08-01")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatePdfReportWithCustomPeriodAndValidDatesShouldReturn200() throws Exception {
        mockUserRepository();
        byte[] fakePdf = "%PDF-1.4 fake content".getBytes();
        when(dashboardReportService.generateReport(
                        eq(1L), eq("CUSTOM"), eq("2026-08-01"), eq("2026-08-25")))
                .thenReturn(fakePdf);

        mockMvc.perform(get("/dashboard/reports/pdf")
                        .param("period", "CUSTOM")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-25")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(fakePdf));
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private void authenticateUser() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "john.doe@test.com", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void mockService(String period, String startDate, String endDate) {
        mockUserRepository();
        when(dashboardService.getDashboardStats(eq(1L), eq(period), eq(startDate), eq(endDate)))
                .thenReturn(sampleResponse);
    }

    private void mockUserRepository() {
        Direction direction = new Direction();
        direction.setDirectionId(1L);
        direction.setName("DSI");

        User user = new User();
        user.setUsersId(1L);
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setEmail("john.doe@test.com");
        user.setDirection(direction);

        when(userRepository.findByEmail("john.doe@test.com")).thenReturn(Optional.of(user));
    }

    private DashboardDataResDto buildSampleResponse() {
        return new DashboardDataResDto(
                "TODAY",
                new DashboardStatsResDto(2, 15, 8, 3, 42),
                new DashboardEvolutionResDto(List.of(
                        new DashboardEvolutionPointResDto("25 ao\u00fbt", 3, 2),
                        new DashboardEvolutionPointResDto("24 ao\u00fbt", 5, 3))),
                new DashboardDistributionResDto(List.of(
                        new DashboardDistributionItemResDto(
                                "A faire", "A faire", 5, "bg-amber-400", "bg-amber-400"),
                        new DashboardDistributionItemResDto(
                                "En cours", "En cours", 4, "bg-blue-500", "bg-blue-500"),
                        new DashboardDistributionItemResDto(
                                "Termine", "Termine", 6, "bg-emerald-500", "bg-emerald-500")),
                        15),
                List.of(),
                List.of());
    }
}
