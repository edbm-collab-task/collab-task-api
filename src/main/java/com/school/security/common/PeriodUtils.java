package com.school.security.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Utilitaire centralisé pour la gestion des périodes du tableau de bord.
 * Évite la duplication de resolvePeriodDates/formatPeriodWithDates
 * présente dans DashboardServiceImpl, AdminDashboardServiceImpl, ProjectReportServiceImpl, etc.
 */
public final class PeriodUtils {

    private PeriodUtils() {}

    /**
     * Résout une période (TODAY, LAST_7_DAYS, etc.) en intervalle [début, fin].
     */
    public static LocalDateTime[] resolvePeriodDates(String period, String startDate, String endDate, LocalDate today) {
        LocalDateTime start;
        LocalDateTime end = today.atTime(LocalTime.MAX);
        String p = period != null ? period : "LAST_30_DAYS";
        switch (p) {
            case "TODAY": start = today.atStartOfDay(); break;
            case "LAST_7_DAYS": start = today.minusDays(6).atStartOfDay(); break;
            case "LAST_30_DAYS": start = today.minusDays(29).atStartOfDay(); break;
            case "LAST_3_MONTHS": start = today.minusMonths(3).atStartOfDay(); break;
            case "THIS_YEAR": start = today.withDayOfYear(1).atStartOfDay(); break;
            case "CUSTOM":
                start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate).atStartOfDay() : today.minusDays(29).atStartOfDay();
                end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : today.atTime(LocalTime.MAX);
                break;
            default: start = today.minusDays(29).atStartOfDay(); break;
        }
        return new LocalDateTime[]{start, end};
    }

    /**
     * Formate une période pour affichage PDF (avec dates si CUSTOM).
     */
    public static String formatPeriodWithDates(String period, String startDate, String endDate) {
        if (period == null) return "";
        return switch (period) {
            case "TODAY" -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS" -> "30 derniers jours";
            case "LAST_3_MONTHS" -> "3 derniers mois";
            case "THIS_YEAR" -> "Cette année";
            case "CUSTOM" -> (startDate != null && endDate != null && !startDate.isEmpty() && !endDate.isEmpty()) ? startDate + " au " + endDate : "Période personnalisée";
            default -> period;
        };
    }
}
