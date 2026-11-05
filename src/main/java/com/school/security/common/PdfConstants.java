package com.school.security.common;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Constantes centralisées pour la génération PDF.
 * Évite la duplication des couleurs/polices entre DashboardReportServiceImpl,
 * AdminDashboardReportServiceImpl et ProjectReportReportServiceImpl.
 */
public final class PdfConstants {
    private PdfConstants() {}

    // Couleurs
    public static final Color COLOR_PRIMARY    = new Color(59, 130, 246);
    public static final Color COLOR_DARK       = new Color(30, 30, 30);
    public static final Color COLOR_HEADER_BG  = new Color(240, 240, 240);
    public static final Color COLOR_HEADER_FG  = Color.BLACK;
    public static final Color COLOR_LIGHT_BG   = new Color(248, 250, 252);
    public static final Color COLOR_BORDER     = new Color(226, 232, 240);
    public static final Color COLOR_MUTED      = new Color(100, 116, 139);
    public static final Color COLOR_GRID       = new Color(241, 245, 249);
    public static final Color COLOR_AMBER      = new Color(245, 158, 11);
    public static final Color COLOR_BLUE       = new Color(59, 130, 246);
    public static final Color COLOR_EMERALD    = new Color(16, 185, 129);
    public static final Color COLOR_RED        = new Color(239, 68, 68);
    public static final Color COLOR_GRAY       = new Color(148, 163, 184);
    public static final Color COLOR_BAR_BG     = new Color(241, 245, 249);
    public static final Color BLACK            = Color.BLACK;
    public static final Color LIGHT_GRAY       = new Color(240, 240, 240);
    public static final Color BORDER_GRAY      = new Color(200, 200, 200);

    // Polices
    public static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, COLOR_DARK);
    public static final Font FONT_ROLE        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Font.NORMAL, COLOR_PRIMARY);
    public static final Font FONT_SUBTITLE    = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, COLOR_MUTED);
    public static final Font FONT_SECTION     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, COLOR_DARK);
    public static final Font FONT_KPI_LABEL   = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COLOR_MUTED);
    public static final Font FONT_KPI_VALUE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, COLOR_DARK);
    public static final Font FONT_TH          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, COLOR_HEADER_FG);
    public static final Font FONT_TD          = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COLOR_DARK);
    public static final Font FONT_SMALL       = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, COLOR_DARK);
    public static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, COLOR_MUTED);
    public static final Font FONT_LEGEND      = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK);
    public static final Font FONT_PERIOD      = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_MUTED);
    public static final Font FONT_CHART_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK);
    public static final Font FONT_CHART_VAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, COLOR_DARK);
    public static final Font FONT_AXIS_LABEL  = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, COLOR_MUTED);
    public static final Font FONT_KPI_LABEL_PROJECT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK);
    public static final Font FONT_KPI_VALUE_PROJECT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, COLOR_DARK);

    // Polices additionnelles pour ProjectReport
    public static final Font FONT_BODY           = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    public static final Font FONT_BOLD_12        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, Color.BLACK);
    public static final Font FONT_BOLD_14        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, Color.BLACK);
    public static final Font FONT_BOLD_10        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL, Color.BLACK);
    public static final Font FONT_ITALIC_9       = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.BLACK);
    public static final Font FONT_FOOTER_DARK    = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.BLACK);

    // Formatters
    public static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    public static final DateTimeFormatter PDF_GENERATED_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter PDF_DATE_LONG = DateTimeFormatter.ofPattern("d MMMM yyyy — HH:mm", Locale.FRENCH);
}
