package com.school.security.services.implementations;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfWriter;
import com.school.security.dtos.responses.*;
import com.school.security.enums.RoleType;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class DashboardReportServiceImpl implements DashboardReportService {

    private DashboardService dashboardService;

    // ─── Colors ─────────────────────────────────────────────────
    private static final Color COLOR_PRIMARY    = new Color(59, 130, 246);
    private static final Color COLOR_DARK       = new Color(30, 30, 30);
    private static final Color COLOR_HEADER_BG  = new Color(59, 130, 246);
    private static final Color COLOR_HEADER_FG  = Color.WHITE;
    private static final Color COLOR_LIGHT_BG   = new Color(248, 250, 252);
    private static final Color COLOR_BORDER     = new Color(226, 232, 240);
    private static final Color COLOR_MUTED      = new Color(100, 116, 139);
    private static final Color COLOR_GRID       = new Color(241, 245, 249);

    private static final Color COLOR_AMBER      = new Color(245, 158, 11);
    private static final Color COLOR_BLUE       = new Color(59, 130, 246);
    private static final Color COLOR_EMERALD    = new Color(16, 185, 129);
    private static final Color COLOR_RED        = new Color(239, 68, 68);
    private static final Color COLOR_GRAY       = new Color(148, 163, 184);

    private static final Color COLOR_BAR_BG     = new Color(241, 245, 249);

    // ─── Fonts ──────────────────────────────────────────────────
    private static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, COLOR_DARK);
    private static final Font FONT_ROLE        = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Font.NORMAL, COLOR_PRIMARY);
    private static final Font FONT_SUBTITLE    = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_SECTION     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD, COLOR_DARK);
    private static final Font FONT_KPI_LABEL   = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_KPI_VALUE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD, COLOR_DARK);
    private static final Font FONT_TH          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, COLOR_HEADER_FG);
    private static final Font FONT_TD          = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COLOR_DARK);
    private static final Font FONT_CHART_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK);
    private static final Font FONT_CHART_VAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, COLOR_DARK);
    private static final Font FONT_CHART_VAL_SM= FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Font.BOLD, COLOR_DARK);
    private static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, COLOR_MUTED);
    private static final Font FONT_LEGEND      = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COLOR_DARK);
    private static final Font FONT_PERIOD      = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_AXIS_LABEL  = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, COLOR_MUTED);

    // ─── Formatters ─────────────────────────────────────────────
    private static final DateTimeFormatter PDF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy — HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter PDF_GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int CHART_MAX_BAR_HEIGHT = 90;

    // ─── Public entry ───────────────────────────────────────────

    @Override
    public byte[] generateReport(Long userId, RoleType role, String period, String startDate, String endDate) {
        DashboardDataResDto data =
                dashboardService.getDashboardStats(userId, period, startDate, endDate);

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
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }

        return out.toByteArray();
    }

    // ─── Header ─────────────────────────────────────────────────

    private void addHeader(Document document, RoleType role, String period) throws DocumentException {
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
        leftCell.addElement(new Phrase("Période : " + formatPeriodWithDates(period), FONT_PERIOD));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Phrase(
                "Généré le " + LocalDateTime.now().format(PDF_GENERATED_FORMAT), FONT_PERIOD));

        metaTable.addCell(leftCell);
        metaTable.addCell(rightCell);
        metaTable.setSpacingAfter(8);
        document.add(metaTable);

        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // ─── KPI Cards ─────────────────────────────────────────────

    private void addKpiCards(Document document, DashboardStatsResDto stats, RoleType role)
            throws DocumentException {
        Paragraph section = new Paragraph("Indicateurs clés", FONT_SECTION);
        section.setSpacingBefore(10);
        section.setSpacingAfter(8);
        document.add(section);

        boolean showUsers = role == RoleType.SUPER_ADMIN || role == RoleType.ADMIN;
        int cols = showUsers ? 5 : 4;

        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);
        table.setWidths(buildKpiWidths(cols));

        addKpiCell(table, "PROJETS", String.valueOf(stats.projects()), COLOR_PRIMARY);
        addKpiCell(table, "TÂCHES", String.valueOf(stats.tasks()), COLOR_BLUE);
        addKpiCell(table, "TERMINÉES", String.valueOf(stats.completedTasks()), COLOR_EMERALD);
        addKpiCell(table, "EN RETARD", String.valueOf(stats.overdueTasks()), COLOR_RED);
        if (showUsers) {
            addKpiCell(table, "INSCRITS", String.valueOf(stats.totalUsers()), COLOR_AMBER);
        }

        table.setSpacingAfter(6);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    private float[] buildKpiWidths(int cols) {
        float[] w = new float[cols];
        float val = 100f / cols;
        for (int i = 0; i < cols; i++) w[i] = val;
        return w;
    }

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

    // ─── Task Status Chart (horizontal bar chart) ───────────────

    private void addTaskStatusChart(Document document, DashboardStatsResDto stats)
            throws DocumentException {
        Paragraph section = new Paragraph("Vue d'ensemble des tâches", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        long total = stats.tasks();
        long completed = stats.completedTasks();
        long overdue = stats.overdueTasks();
        long remaining = Math.max(0, total - completed);

        if (total == 0) {
            document.add(new Paragraph("Aucune tâche à afficher.", FONT_CHART_LABEL));
            addHorizontalRule(document, COLOR_BORDER, 0.5f);
            return;
        }

        long maxVal = Math.max(total, Math.max(completed, Math.max(overdue, remaining)));

        String[] labels = {"Total", "Terminées", "En retard", "Restantes"};
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

    // ─── Distribution Chart (horizontal bar chart) ──────────────

    private void addDistributionChart(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.distribution() == null || data.distribution().items() == null
                || data.distribution().items().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Répartition des tâches par statut", FONT_SECTION);
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

    private PdfPCell buildBarCell(long value, long maxVal, Color color) throws DocumentException {
        float barPct = maxVal > 0 ? (float) value / maxVal : 0;
        float remainingPct = 1f - barPct;

        PdfPTable barWrapper = new PdfPTable(2);
        barWrapper.setWidthPercentage(100);
        barWrapper.setWidths(new float[]{barPct * 100f, remainingPct * 100f + 0.01f});

        PdfPCell barFiller = new PdfPCell();
        barFiller.setBorder(Rectangle.NO_BORDER);
        barFiller.setBackgroundColor(color);
        barFiller.setFixedHeight(14);
        barFiller.setMinimumHeight(14);
        barWrapper.addCell(barFiller);

        PdfPCell emptyFiller = new PdfPCell();
        emptyFiller.setBorder(Rectangle.NO_BORDER);
        emptyFiller.setBackgroundColor(COLOR_BAR_BG);
        emptyFiller.setFixedHeight(14);
        barWrapper.addCell(emptyFiller);

        PdfPCell outerCell = new PdfPCell(barWrapper);
        outerCell.setBorder(Rectangle.NO_BORDER);
        outerCell.setPadding(4);
        return outerCell;
    }

    // ─── Evolution Chart (vertical grouped bar chart) ───────────

    private void addEvolutionChart(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.evolution() == null || data.evolution().points() == null
                || data.evolution().points().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Évolution des tâches", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        List<DashboardEvolutionPointResDto> points = data.evolution().points();

        int maxVal = (int) points.stream()
                .mapToLong(p -> Math.max(p.created(), p.completed()))
                .max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);

        PdfPTable chart = new PdfPTable(2);
        chart.setWidthPercentage(100);
        chart.setWidths(new float[]{85f, 15f});

        // Bars column
        PdfPCell barsCell = new PdfPCell();
        barsCell.setBorder(Rectangle.NO_BORDER);
        barsCell.setPadding(4);

        PdfPTable barsTable = new PdfPTable(1);
        barsTable.setWidthPercentage(100);

        for (DashboardEvolutionPointResDto point : points) {
            PdfPTable barPair = new PdfPTable(2);
            barPair.setWidthPercentage(100);
            barPair.setWidths(new float[]{50f, 50f});

            int cH = maxVal > 0 ? (int) Math.round((double) point.created() / maxVal * CHART_MAX_BAR_HEIGHT) : 0;
            int pH = maxVal > 0 ? (int) Math.round((double) point.completed() / maxVal * CHART_MAX_BAR_HEIGHT) : 0;

            PdfPCell cBar = new PdfPCell();
            cBar.setBorder(Rectangle.NO_BORDER);
            cBar.addElement(createVerticalBar(Math.max(1, cH), COLOR_BLUE));
            barPair.addCell(cBar);

            PdfPCell pBar = new PdfPCell();
            pBar.setBorder(Rectangle.NO_BORDER);
            pBar.addElement(createVerticalBar(Math.max(1, pH), COLOR_EMERALD));
            barPair.addCell(pBar);

            PdfPCell wrapper = new PdfPCell(barPair);
            wrapper.setBorder(Rectangle.NO_BORDER);
            wrapper.setPadding(1);
            barsTable.addCell(wrapper);
        }
        barsCell.addElement(barsTable);
        chart.addCell(barsCell);

        // Labels column
        PdfPCell labelsCell = new PdfPCell();
        labelsCell.setBorder(Rectangle.NO_BORDER);
        labelsCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        labelsCell.setPadding(4);

        for (DashboardEvolutionPointResDto point : points) {
            Paragraph lbl = new Paragraph(point.label(), FONT_AXIS_LABEL);
            lbl.setAlignment(Element.ALIGN_CENTER);
            lbl.setSpacingBefore(1);
            lbl.setSpacingAfter(8);
            labelsCell.addElement(lbl);
        }
        chart.addCell(labelsCell);

        outer.addCell(chart);
        outer.setSpacingAfter(6);
        document.add(outer);

        // Legend
        PdfPTable legend = new PdfPTable(2);
        legend.setWidthPercentage(100);

        PdfPCell l1 = new PdfPCell();
        l1.setBorder(Rectangle.NO_BORDER);
        l1.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p1 = new Paragraph();
        p1.add(new Phrase("■ ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_BLUE)));
        p1.add(new Phrase("Créées", FONT_LEGEND));
        l1.addElement(p1);
        legend.addCell(l1);

        PdfPCell l2 = new PdfPCell();
        l2.setBorder(Rectangle.NO_BORDER);
        l2.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph();
        p2.add(new Phrase("■ ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_EMERALD)));
        p2.add(new Phrase("Terminées", FONT_LEGEND));
        l2.addElement(p2);
        legend.addCell(l2);

        legend.setSpacingAfter(8);
        document.add(legend);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    private PdfPTable createVerticalBar(int height, Color color) throws DocumentException {
        PdfPTable bar = new PdfPTable(1);
        bar.setWidthPercentage(100);
        bar.setWidths(new float[]{100f});

        PdfPCell filler = new PdfPCell();
        filler.setBorder(Rectangle.NO_BORDER);
        filler.setFixedHeight(height);
        filler.setMinimumHeight(height);
        filler.setBackgroundColor(color);
        bar.addCell(filler);

        return bar;
    }

    // ─── Recent Activity Table ──────────────────────────────────

    private void addRecentActivityTable(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentActivity() == null || data.recentActivity().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Activité récente", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{28f, 22f, 28f, 22f});
        table.setWidthPercentage(100);

        addTableHeader(table, "Date");
        addTableHeader(table, "Activité");
        addTableHeader(table, "Détail");
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

    // ─── Projects Table ─────────────────────────────────────────

    private void addProjectsTable(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentProjects() == null || data.recentProjects().isEmpty()) {
            return;
        }

        Paragraph section = new Paragraph("Projets récents", FONT_SECTION);
        section.setSpacingBefore(12);
        section.setSpacingAfter(8);
        document.add(section);

        PdfPTable table = new PdfPTable(new float[]{40f, 30f, 15f, 15f});
        table.setWidthPercentage(100);

        addTableHeader(table, "Projet");
        addTableHeader(table, "Propriétaire");
        addTableHeader(table, "Progression");
        addTableHeader(table, "Statut");

        for (DashboardRecentProjectResDto p : data.recentProjects()) {
            addTableRow(table, p.title());
            addTableRow(table, p.ownerName());
            addTableRow(table, p.progress() + "%");
            addTableRow(table, p.progress() >= 100 ? "Terminé" : "En cours");
        }

        table.setSpacingAfter(8);
        document.add(table);
        addHorizontalRule(document, COLOR_BORDER, 0.5f);
    }

    // ─── Footer ─────────────────────────────────────────────────

    private void addFooter(Document document, RoleType role, PdfWriter writer) throws DocumentException {
        addPageNumberFooter(writer, document);

        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Phrase(
                "Rapport " + resolveRoleLabel(role).toLowerCase() + " — Collab Task",
                FONT_FOOTER));

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Phrase(
                "Document généré automatiquement",
                FONT_FOOTER));

        footerTable.addCell(left);
        footerTable.addCell(right);
        footerTable.setSpacingBefore(12);
        document.add(footerTable);
    }

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

    // ─── Shared table helpers ───────────────────────────────────

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(COLOR_HEADER_BG);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addTableRow(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TD));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addHorizontalRule(Document document, Color color, float thickness)
            throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(color);
        cell.setBorderWidthBottom(thickness);
        cell.setFixedHeight(1);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.addElement(new Phrase(" "));
        line.addCell(cell);
        line.setSpacingAfter(4);
        document.add(line);
    }

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
            p.add(new Phrase("■ ", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, resolveStatusColor(item.name()))));
            p.add(new Phrase(item.name() + " (" + item.count() + ")", FONT_LEGEND));
            cell.addElement(p);
            legend.addCell(cell);
        }
        legend.setSpacingAfter(8);
        document.add(legend);
    }

    // ─── Resolution helpers ─────────────────────────────────────

    private String formatActivityDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(PDF_DATE_FORMAT);
    }

    private String translateActivityType(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase()) {
            case "TASK_CREATED"    -> "Tâche créée";
            case "TASK_COMPLETED"  -> "Tâche terminée";
            case "TASK_UPDATED"    -> "Tâche modifiée";
            case "TASK_DELETED"    -> "Tâche supprimée";
            case "PROJECT_CREATED" -> "Projet créé";
            case "PROJECT_UPDATED" -> "Projet modifié";
            case "USER_ADDED"      -> "Utilisateur ajouté";
            case "COMMENT_ADDED"   -> "Commentaire ajouté";
            default                -> type;
        };
    }

    private String resolveTitle(RoleType role) {
        return switch (role) {
            case SUPER_ADMIN -> "Rapport global de la plateforme";
            case ADMIN        -> "Rapport d'administration";
            case USER         -> "Mon rapport d'activité";
        };
    }

    private String resolveRoleLabel(RoleType role) {
        return switch (role) {
            case SUPER_ADMIN -> "SUPER ADMINISTRATEUR";
            case ADMIN        -> "ADMINISTRATEUR";
            case USER         -> "UTILISATEUR";
        };
    }

    private Color resolveStatusColor(String statusName) {
        return switch (statusName) {
            case "A faire"   -> COLOR_AMBER;
            case "En cours"  -> COLOR_BLUE;
            case "Terminé", "Termine" -> COLOR_EMERALD;
            default          -> COLOR_GRAY;
        };
    }

    private String formatPeriodWithDates(String period) {
        return switch (period) {
            case "TODAY"       -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS"-> "30 derniers jours";
            case "LAST_3_MONTHS"-> "3 derniers mois";
            case "THIS_YEAR"   -> "Cette année";
            case "CUSTOM"      -> "Période personnalisée";
            default            -> period;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
