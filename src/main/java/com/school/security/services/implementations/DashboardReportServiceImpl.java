package com.school.security.services.implementations;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfWriter;
import com.school.security.common.ChartRenderer;
import com.school.security.common.PdfConstants;
import com.school.security.common.PdfHelper;
import com.school.security.common.PeriodUtils;
import com.school.security.dtos.responses.*;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ImplÃƒÂ©mentation de la gÃƒÂ©nÃƒÂ©ration du rapport PDF du tableau de bord (iText /
 * OpenPDF).
 *
 * <p>Fonctionnement constatÃƒÂ© (documentÃƒÂ©, non modifiÃƒÂ©) :
 * <ul>
 *   <li>les donnÃƒÂ©es proviennent de {@link DashboardService#getDashboardStats}
 *       (mÃƒÂªmes rÃƒÂ¨gles de pÃƒÂ©rimÃƒÂ¨tre que le tableau de bord web) ; c'est le rÃƒÂ´le
 *       transmis qui dÃƒÂ©termine le titre et l'affichage de la carte "INSCRITS"
 *       (SUPER_ADMIN/ADMIN uniquement) ;</li>
 *   <li>le document enchaÃƒÂ®ne : en-tÃƒÂªte, cartes KPI, graphique de statut des
 *       tÃƒÂ¢ches, rÃƒÂ©partition par statut, courbe d'ÃƒÂ©volution, tableau d'activitÃƒÂ©
 *       rÃƒÂ©cente, tableau des projets rÃƒÂ©cents et pied de page ;</li>
 *   <li>les couleurs/polices sont centralisÃƒÂ©es dans {@code PdfConstants} et les
 *       helpers de table/barre dÃƒÂ©lÃƒÂ©guÃƒÂ©s ÃƒÂ  {@code PdfHelper} / {@code ChartRenderer} ;</li>
 *   <li>{@code computeNiceMax} est dÃƒÂ©clarÃƒÂ© mais jamais appelÃƒÂ© (code mort).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class DashboardReportServiceImpl implements DashboardReportService {

    private DashboardService dashboardService;

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Colors centralisÃƒÂ©es dans PdfConstants Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    private static final Color COLOR_PRIMARY    = PdfConstants.COLOR_PRIMARY;
    private static final Color COLOR_DARK       = PdfConstants.COLOR_DARK;
    private static final Color COLOR_HEADER_BG  = PdfConstants.COLOR_HEADER_BG;
    private static final Color COLOR_HEADER_FG  = PdfConstants.COLOR_HEADER_FG;
    private static final Color COLOR_LIGHT_BG   = PdfConstants.COLOR_LIGHT_BG;
    private static final Color COLOR_BORDER     = PdfConstants.COLOR_BORDER;
    private static final Color COLOR_MUTED      = PdfConstants.COLOR_MUTED;
    private static final Color COLOR_GRID       = PdfConstants.COLOR_GRID;

    private static final Color COLOR_AMBER      = PdfConstants.COLOR_AMBER;
    private static final Color COLOR_BLUE       = PdfConstants.COLOR_BLUE;
    private static final Color COLOR_EMERALD    = PdfConstants.COLOR_EMERALD;
    private static final Color COLOR_RED        = PdfConstants.COLOR_RED;
    private static final Color COLOR_GRAY       = PdfConstants.COLOR_GRAY;

    private static final Color COLOR_BAR_BG     = PdfConstants.COLOR_BAR_BG;

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Fonts centralisÃƒÂ©es Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    private static final Font FONT_TITLE       = PdfConstants.FONT_TITLE;
    private static final Font FONT_ROLE        = PdfConstants.FONT_ROLE;
    private static final Font FONT_SUBTITLE    = PdfConstants.FONT_SUBTITLE;
    private static final Font FONT_SECTION     = PdfConstants.FONT_SECTION;
    private static final Font FONT_KPI_LABEL   = PdfConstants.FONT_KPI_LABEL;
    private static final Font FONT_KPI_VALUE   = PdfConstants.FONT_KPI_VALUE;
    private static final Font FONT_TH          = PdfConstants.FONT_TH;
    private static final Font FONT_TD          = PdfConstants.FONT_TD;
    private static final Font FONT_CHART_LABEL = PdfConstants.FONT_CHART_LABEL;
    private static final Font FONT_CHART_VAL   = PdfConstants.FONT_CHART_VAL;
    private static final Font FONT_FOOTER      = PdfConstants.FONT_FOOTER;
    private static final Font FONT_LEGEND      = PdfConstants.FONT_LEGEND;
    private static final Font FONT_PERIOD      = PdfConstants.FONT_PERIOD;
    private static final DateTimeFormatter PDF_DATE_FORMAT = PdfConstants.PDF_DATE_LONG;
    private static final DateTimeFormatter PDF_GENERATED_FORMAT = PdfConstants.PDF_GENERATED_FORMAT;

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Public entry Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /**
     * GÃƒÂ©nÃƒÂ¨re le PDF complet en assemblant les sections dans l'ordre. Toute
     * erreur iText est encapsulÃƒÂ©e dans une {@code RuntimeException}.
     */
    @Override
    public byte[] generateReport(Long userId, String role, String period, String startDate, String endDate, Long projectId) {
        DashboardDataResDto data =
                dashboardService.getDashboardStats(userId, period, startDate, endDate, projectId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 45, 45, 40, 45);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, role, period);
            addKpiCards(document, data.stats(), role);
            addTaskStatusChart(document, data.stats());
            addDistributionChart(document, data);
            addEvolutionChart(document, data);
            addRecentActivityTable(document, data);
            addProjectsTable(document, data);
            addFooter(document, role, writer);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la gÃƒÂ©nÃƒÂ©ration du rapport PDF", e);
        }

        return out.toByteArray();
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Header Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** En-tÃƒÂªte : titre selon le rÃƒÂ´le, pÃƒÂ©riode et date de gÃƒÂ©nÃƒÂ©ration. */
    private void addHeader(Document document, String role, String period) throws DocumentException {
        Paragraph title = new Paragraph(resolveTitle(role), FONT_TITLE);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph roleLine = new Paragraph(resolveRoleLabel(role), FONT_ROLE);
        roleLine.setAlignment(Element.ALIGN_LEFT);
        roleLine.setSpacingAfter(8);
        document.add(roleLine);

        Paragraph appLine = new Paragraph("Collab Task", FONT_SUBTITLE);
        appLine.setAlignment(Element.ALIGN_LEFT);
        appLine.setSpacingAfter(12);
        document.add(appLine);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{60f, 40f});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Phrase("PÃƒÂ©riode : " + formatPeriodWithDates(period), FONT_PERIOD));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Phrase(
                "GÃƒÂ©nÃƒÂ©rÃƒÂ© le " + LocalDateTime.now().format(PDF_GENERATED_FORMAT), FONT_PERIOD));

        metaTable.addCell(leftCell);
        metaTable.addCell(rightCell);
        metaTable.setSpacingAfter(8);
        document.add(metaTable);

        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ KPI Cards Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /**
     * Cartes KPI ; la carte "INSCRITS" (nombre total d'utilisateurs) n'est
     * affichÃƒÂ©e que pour SUPER_ADMIN et ADMIN (5 colonnes au lieu de 4).
     */
    private void addKpiCards(Document document, DashboardStatsResDto stats, String role)
            throws DocumentException {
        Paragraph section = new Paragraph("Indicateurs clÃƒÂ©s", FONT_SECTION);
        section.setSpacingBefore(10);
        section.setSpacingAfter(8);
        document.add(section);

        boolean showUsers = "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
        int cols = showUsers ? 5 : 4;

        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setWidths(buildKpiWidths(cols));

        addKpiCell(table, "PROJETS", String.valueOf(stats.projects()), COLOR_PRIMARY);
        addKpiCell(table, "TÃƒâ€šCHES", String.valueOf(stats.tasks()), COLOR_BLUE);
        addKpiCell(table, "TERMINÃƒâ€°ES", String.valueOf(stats.completedTasks()), COLOR_EMERALD);
        addKpiCell(table, "EN RETARD", String.valueOf(stats.overdueTasks()), COLOR_RED);
        if (showUsers) {
            addKpiCell(table, "INSCRITS", String.valueOf(stats.totalUsers()), COLOR_AMBER);
        }

        table.setSpacingAfter(6);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    /** Largeurs ÃƒÂ©gales pour les {@code cols} cartes KPI. */
    private float[] buildKpiWidths(int cols) {
        float[] w = new float[cols];
        float val = 100f / cols;
        for (int i = 0; i < cols; i++) w[i] = val;
        return w;
    }

    /** Ajoute une carte KPI (valeur puis libellÃƒÂ©) ; le paramÃƒÂ¨tre {@code accent} n'est pas utilisÃƒÂ©. */
    private void addKpiCell(PdfPTable table, String label, String value, Color accent) {
        PdfPCell card = new PdfPCell();
        card.setPadding(10);
        card.setBorder(Rectangle.NO_BORDER);
        card.setBackgroundColor(COLOR_LIGHT_BG);

        Paragraph valP = new Paragraph(value, FONT_KPI_VALUE);
        valP.setAlignment(Element.ALIGN_CENTER);
        valP.setSpacingAfter(2);
        card.addElement(valP);

        Paragraph lblP = new Paragraph(label, FONT_KPI_LABEL);
        lblP.setAlignment(Element.ALIGN_CENTER);
        card.addElement(lblP);

        table.addCell(card);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Task Status Chart (horizontal bar chart) Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /**
     * Barres horizontales Total / TerminÃƒÂ©es / En retard / Restantes ; affiche un
     * message si aucune tÃƒÂ¢che.
     */
    private void addTaskStatusChart(Document document, DashboardStatsResDto stats)
            throws DocumentException {
        Paragraph section = new Paragraph("Vue d'ensemble des tÃƒÂ¢ches", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        long total = stats.tasks();
        long completed = stats.completedTasks();
        long overdue = stats.overdueTasks();
        long remaining = Math.max(0, total - completed);

        if (total == 0) {
            document.add(new Paragraph("Aucune tÃƒÂ¢che ÃƒÂ  afficher.", FONT_CHART_LABEL));
            addHorizontalRule(document, COLOR_BORDER, 0.5f);
            return;
        }

        long maxVal = Math.max(total, Math.max(completed, Math.max(overdue, remaining)));

        String[] labels = {"Total", "TerminÃƒÂ©es", "En retard", "Restantes"};
        long[] values = {total, completed, overdue, remaining};
        Color[] colors = {COLOR_BLUE, COLOR_EMERALD, COLOR_RED, COLOR_GRAY};

        PdfPTable table = new PdfPTable(new float[]{22f, 50f, 13f, 15f});
        table.setWidthPercentage(100);
        table.setTotalWidth(new float[]{110f, 250f, 65f, 75f});

        for (int i = 0; i < labels.length; i++) {
            PdfPCell labelCell = new PdfPCell(new Phrase(labels[i], FONT_CHART_LABEL));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPadding(5);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(labelCell);

            PdfPCell barCell = buildBarCell(values[i], maxVal, colors[i]);
            table.addCell(barCell);

            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(emptyCell);

            PdfPCell valCell = new PdfPCell(new Phrase(String.valueOf(values[i]), FONT_CHART_VAL));
            valCell.setBorder(Rectangle.NO_BORDER);
            valCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            valCell.setPadding(5);
            valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(valCell);
        }

        table.setSpacingAfter(8);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Distribution Chart (horizontal bar chart) Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** RÃƒÂ©partition par statut avec barres, ligne Total et lÃƒÂ©gende ; ignorÃƒÂ©e si vide. */
    private void addDistributionChart(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.distribution() == null || data.distribution().items() == null
                || data.distribution().items().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("RÃƒÂ©partition des tÃƒÂ¢ches par statut", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        List<DashboardDistributionItemResDto> items = data.distribution().items();
        long maxCount = items.stream().mapToLong(DashboardDistributionItemResDto::count).max().orElse(1);
        if (maxCount == 0) maxCount = 1;

        PdfPTable table = new PdfPTable(new float[]{22f, 50f, 13f, 15f});
        table.setWidthPercentage(100);
        table.setTotalWidth(new float[]{110f, 250f, 65f, 75f});

        for (DashboardDistributionItemResDto item : items) {
            Color barColor = resolveStatusColor(item.name());

            PdfPCell labelCell = new PdfPCell(new Phrase(item.name(), FONT_CHART_LABEL));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setPadding(5);
            labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(labelCell);

            PdfPCell barCell = buildBarCell(item.count(), maxCount, barColor);
            table.addCell(barCell);

            PdfPCell emptyCell = new PdfPCell();
            emptyCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(emptyCell);

            PdfPCell valCell = new PdfPCell(new Phrase(String.valueOf(item.count()), FONT_CHART_VAL));
            valCell.setBorder(Rectangle.NO_BORDER);
            valCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            valCell.setPadding(5);
            valCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(valCell);
        }

        // Total row
        PdfPCell totalLabel = new PdfPCell(new Phrase("Total", FONT_TH));
        totalLabel.setBorder(Rectangle.NO_BORDER);
        totalLabel.setBackgroundColor(COLOR_HEADER_BG);
        totalLabel.setPadding(5);
        table.addCell(totalLabel);

        PdfPCell totalBar = new PdfPCell();
        totalBar.setBorder(Rectangle.NO_BORDER);
        totalBar.setBackgroundColor(COLOR_HEADER_BG);
        table.addCell(totalBar);
        PdfPCell totalSpacer = new PdfPCell();
        totalSpacer.setBorder(Rectangle.NO_BORDER);
        totalSpacer.setBackgroundColor(COLOR_HEADER_BG);
        table.addCell(totalSpacer);

        PdfPCell totalVal = new PdfPCell(new Phrase(String.valueOf(data.distribution().total()), FONT_TH));
        totalVal.setBorder(Rectangle.NO_BORDER);
        totalVal.setHorizontalAlignment(Element.ALIGN_LEFT);
        totalVal.setBackgroundColor(COLOR_HEADER_BG);
        totalVal.setPadding(5);
        table.addCell(totalVal);

        table.setSpacingAfter(6);
        document.add(table);

        addStatusLegend(document, items);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    /** DÃƒÂ©lÃƒÂ¨gue la crÃƒÂ©ation d'une cellule de barre ÃƒÂ  PdfHelper. */
    private PdfPCell buildBarCell(long value, long maxVal, Color color) {
        return PdfHelper.buildBarCell(value, maxVal, color);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Evolution Chart (line chart, rendered as flow image) Ã¢â€â‚¬Ã¢â€â‚¬

    /**
     * Courbe d'ÃƒÂ©volution rendue sous forme d'image, avec titre et lÃƒÂ©gende
     * maintenus ensemble sur la mÃƒÂªme page ; ignorÃƒÂ©e si vide.
     */
    private void addEvolutionChart(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.evolution() == null || data.evolution().points() == null
                || data.evolution().points().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Ãƒâ€°volution des tÃƒÂ¢ches", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);

        Image chartImage = renderLineChart(data.evolution().points());

        // Keep the heading, chart and legend together on the same page.
        PdfPTable sectionGroup = new PdfPTable(1);
        sectionGroup.setWidthPercentage(100);
        sectionGroup.setKeepTogether(true);

        PdfPCell groupCell = new PdfPCell();
        groupCell.setBorder(Rectangle.NO_BORDER);
        groupCell.setPadding(0);
        groupCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        groupCell.addElement(section);
        groupCell.addElement(chartImage);

        // Legend
        PdfPTable legend = new PdfPTable(2);
        legend.setWidthPercentage(100);

        PdfPCell l1 = new PdfPCell();
        l1.setBorder(Rectangle.NO_BORDER);
        l1.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph();
        p1.add(new Phrase("Ã¢â€”Â ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_BLUE)));
        p1.add(new Phrase("CrÃƒÂ©ÃƒÂ©es", FONT_LEGEND));
        l1.addElement(p1);
        legend.addCell(l1);

        PdfPCell l2 = new PdfPCell();
        l2.setBorder(Rectangle.NO_BORDER);
        l2.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph();
        p2.add(new Phrase("Ã¢â€”Â ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_EMERALD)));
        p2.add(new Phrase("TerminÃƒÂ©es", FONT_LEGEND));
        l2.addElement(p2);
        legend.addCell(l2);

        groupCell.addElement(legend);

        sectionGroup.addCell(groupCell);
        sectionGroup.setSpacingAfter(8);
        document.add(sectionGroup);

        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    /** DÃƒÂ©lÃƒÂ¨gue le rendu de la courbe ÃƒÂ  ChartRenderer. */
    private Image renderLineChart(List<DashboardEvolutionPointResDto> points) {
        return ChartRenderer.renderLineChart(points);
    }

    /** DÃƒÂ©lÃƒÂ¨gue ÃƒÂ  ChartRenderer (dÃƒÂ©clarÃƒÂ© mais jamais appelÃƒÂ© dans ce service). */
    private int computeNiceMax(int value) {
        return ChartRenderer.computeNiceMax(value);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Recent Activity Table Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** Tableau de l'activitÃƒÂ© rÃƒÂ©cente ; descriptions tronquÃƒÂ©es ; ignorÃƒÂ© si vide. */
    private void addRecentActivityTable(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentActivity() == null || data.recentActivity().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("ActivitÃƒÂ© rÃƒÂ©cente", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{28f, 22f, 28f, 22f});
        table.setWidthPercentage(100);

        addTableHeader(table, "Date");
        addTableHeader(table, "ActivitÃƒÂ©");
        addTableHeader(table, "DÃƒÂ©tail");
        addTableHeader(table, "Utilisateur");

        for (DashboardActivityItemResDto a : data.recentActivity()) {
            addTableRow(table, formatActivityDate(a.createdAt()));
            addTableRow(table, translateActivityType(a.type()));
            addTableRow(table, truncate(a.description(), 35));
            addTableRow(table, a.userName() != null ? a.userName() : "");
        }

        table.setSpacingAfter(8);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Projects Table Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** Tableau des projets rÃƒÂ©cents avec progression ; ignorÃƒÂ© si vide. */
    private void addProjectsTable(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentProjects() == null || data.recentProjects().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Projets rÃƒÂ©cents", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{40f, 30f, 15f, 15f});
        table.setWidthPercentage(100);

        addTableHeader(table, "Projet");
        addTableHeader(table, "PropriÃƒÂ©taire");
        addTableHeader(table, "Progression");
        addTableHeader(table, "Statut");

        for (DashboardRecentProjectResDto p : data.recentProjects()) {
            addTableRow(table, p.title());
            addTableRow(table, p.ownerName());
            addTableRow(table, p.progress() + "%");
            addTableRow(table, p.progress() >= 100 ? "TerminÃƒÂ©" : "En cours");
        }

        table.setSpacingAfter(8);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Footer Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** Pied de page : numÃƒÂ©ro de page plus libellÃƒÂ© de rÃƒÂ´le et mention de gÃƒÂ©nÃƒÂ©ration. */
    private void addFooter(Document document, String role, PdfWriter writer) throws DocumentException {
        addPageNumberFooter(writer, document);

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Phrase(
                "Rapport " + resolveRoleLabel(role).toLowerCase() + " Ã¢â‚¬â€ Collab Task",
                FONT_FOOTER));

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Phrase(
                "Document gÃƒÂ©nÃƒÂ©rÃƒÂ© automatiquement",
                FONT_FOOTER));

        footerTable.addCell(left);
        footerTable.addCell(right);
        footerTable.setSpacingBefore(12);
        document.add(footerTable);
    }

    /** Ãƒâ€°crit le numÃƒÂ©ro de page centrÃƒÂ© au bas de la page courante. */
    private void addPageNumberFooter(PdfWriter writer, Document document) throws DocumentException {
        PdfContentByte cb = writer.getDirectContent();
        Paragraph footer = new Paragraph(
                "Page " + writer.getPageNumber(),
                FONT_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                document.getPageSize().getWidth() / 2,
                document.bottomMargin() / 2, 0);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Shared table helpers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** DÃƒÂ©lÃƒÂ¨gue l'en-tÃƒÂªte de tableau ÃƒÂ  PdfHelper. */
    private void addTableHeader(PdfPTable table, String text) {
        PdfHelper.addTableHeader(table, text);
    }

    /** DÃƒÂ©lÃƒÂ¨gue la ligne de tableau ÃƒÂ  PdfHelper. */
    private void addTableRow(PdfPTable table, String text) {
        PdfHelper.addTableRow(table, text);
    }

    /** DÃƒÂ©lÃƒÂ¨gue le trait de sÃƒÂ©paration ÃƒÂ  PdfHelper. */
    private void addHorizontalRule(Document document, Color color, float thickness)
            throws DocumentException {
        PdfHelper.addHorizontalRule(document, color, thickness);
    }

    /** LÃƒÂ©gende du graphique de rÃƒÂ©partition (une pastille colorÃƒÂ©e par statut). */
    private void addStatusLegend(Document document, List<DashboardDistributionItemResDto> items)
            throws DocumentException {
        PdfPTable legend = new PdfPTable(items.size());
        legend.setWidthPercentage(100);
        for (DashboardDistributionItemResDto item : items) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(2);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph p = new Paragraph();
            p.add(new Phrase("Ã¢â€“Â  ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, resolveStatusColor(item.name()))));
            p.add(new Phrase(item.name() + " (" + item.count() + ")", FONT_LEGEND));
            cell.addElement(p);
            legend.addCell(cell);
        }
        legend.setSpacingAfter(8);
        document.add(legend);
    }

    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Resolution helpers Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬

    /** Formate une date d'activitÃƒÂ© ; chaÃƒÂ®ne vide si nulle. */
    private String formatActivityDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(PDF_DATE_FORMAT);
    }

    /** Traduit un type d'activitÃƒÂ© en franÃƒÂ§ais ; type inconnu retournÃƒÂ© tel quel. */
    private String translateActivityType(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase()) {
            case "TASK_CREATED"    -> "TÃƒÂ¢che crÃƒÂ©ÃƒÂ©e";
            case "TASK_COMPLETED"  -> "TÃƒÂ¢che terminÃƒÂ©e";
            case "TASK_UPDATED"    -> "TÃƒÂ¢che modifiÃƒÂ©e";
            case "TASK_DELETED"    -> "TÃƒÂ¢che supprimÃƒÂ©e";
            case "PROJECT_CREATED" -> "Projet crÃƒÂ©ÃƒÂ©";
            case "PROJECT_UPDATED" -> "Projet modifiÃƒÂ©";
            case "USER_ADDED"      -> "Utilisateur ajoutÃƒÂ©";
            case "COMMENT_ADDED"   -> "Commentaire ajoutÃƒÂ©";
            default                -> type;
        };
    }

    /** Titre du rapport selon le rÃƒÂ´le gÃƒÂ©nÃƒÂ©rateur. */
    private String resolveTitle(String role) {
        return switch (role) {
            case "SUPER_ADMIN" -> "Rapport global de la plateforme";
            case "ADMIN" -> "Rapport d'administration";
            case "USER" -> "Mon rapport d'activitÃƒÂ©";
            default -> "Mon rapport d'activité";
        };
    }

    /** LibellÃƒÂ© du rÃƒÂ´le en majuscules pour l'en-tÃƒÂªte et le pied de page. */
    private String resolveRoleLabel(String role) {
        return switch (role) {
            case "SUPER_ADMIN" -> "SUPER ADMINISTRATEUR";
            case "ADMIN" -> "ADMINISTRATEUR";
            case "USER" -> "UTILISATEUR";
            default -> "UTILISATEUR";
        };
    }

    /** Couleur associÃƒÂ©e ÃƒÂ  un statut ("TerminÃƒÂ©" et "Termine" partagent le vert) ; gris par dÃƒÂ©faut. */
    private Color resolveStatusColor(String statusName) {
        return switch (statusName) {
            case "A faire"   -> COLOR_AMBER;
            case "En cours"  -> COLOR_BLUE;
            case "TerminÃƒÂ©", "Termine" -> COLOR_EMERALD;
            default          -> COLOR_GRAY;
        };
    }

    /** LibellÃƒÂ© franÃƒÂ§ais de la pÃƒÂ©riode (sans les dates). */
    private String formatPeriodWithDates(String period) {
        return switch (period) {
            case "TODAY"       -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS"-> "30 derniers jours";
            case "LAST_3_MONTHS"-> "3 derniers mois";
            case "THIS_YEAR"   -> "Cette annÃƒÂ©e";
            case "CUSTOM"      -> "PÃƒÂ©riode personnalisÃƒÂ©e";
            default            -> period;
        };
    }

    /** Tronque un texte ÃƒÂ  {@code maxLength} caractÃƒÂ¨res en suffixant "...". */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}


