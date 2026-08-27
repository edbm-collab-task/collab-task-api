package com.school.security.services.implementations;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.security.dtos.responses.DashboardActivityItemResDto;
import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.dtos.responses.DashboardDistributionItemResDto;
import com.school.security.dtos.responses.DashboardRecentProjectResDto;
import com.school.security.dtos.responses.DashboardStatsResDto;
import com.school.security.services.contracts.DashboardReportService;
import com.school.security.services.contracts.DashboardService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class DashboardReportServiceImpl implements DashboardReportService {

    private DashboardService dashboardService;

    private static final Font TITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD);
    private static final Font SUBTITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL);
    private static final Font SECTION_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.BOLD);
    private static final Font HEADER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
    private static final Font BODY_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font FOOTER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC);

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] generateReport(Long userId, String period, String startDate, String endDate) {
        DashboardDataResDto data =
                dashboardService.getDashboardStats(userId, period, startDate, endDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, period);
            addStatsSection(document, data.stats());
            addEvolutionSection(document, data);
            addDistributionSection(document, data);
            addActivitySection(document, data);
            addProjectsSection(document, data);
            addFooter(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, String period) throws DocumentException {
        Paragraph title = new Paragraph("Rapport Statistique", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        document.add(title);

        Paragraph appName = new Paragraph("Collab Task", SUBTITLE_FONT);
        appName.setAlignment(Element.ALIGN_CENTER);
        appName.setSpacingAfter(15);
        document.add(appName);

        Paragraph periodLine = new Paragraph("Période : " + formatPeriod(period), SUBTITLE_FONT);
        periodLine.setAlignment(Element.ALIGN_CENTER);
        periodLine.setSpacingAfter(5);
        document.add(periodLine);

        Paragraph dateLine =
                new Paragraph(
                        "Généré le : " + LocalDateTime.now().format(DISPLAY_FORMAT), SUBTITLE_FONT);
        dateLine.setAlignment(Element.ALIGN_CENTER);
        dateLine.setSpacingAfter(20);
        document.add(dateLine);

        addSeparator(document);
    }

    private void addStatsSection(Document document, DashboardStatsResDto stats)
            throws DocumentException {
        Paragraph sectionTitle = new Paragraph("1. Indicateurs clés", SECTION_FONT);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addHeaderCell(table, "Indicateur");
        addHeaderCell(table, "Valeur");

        addDataCell(table, "Projets");
        addDataCell(table, String.valueOf(stats.projects()));

        addDataCell(table, "Tâches totales");
        addDataCell(table, String.valueOf(stats.tasks()));

        addDataCell(table, "Tâches terminées");
        addDataCell(table, String.valueOf(stats.completedTasks()));

        addDataCell(table, "Tâches en retard");
        addDataCell(table, String.valueOf(stats.overdueTasks()));

        addDataCell(table, "Utilisateurs inscrits");
        addDataCell(table, String.valueOf(stats.totalUsers()));

        document.add(table);
    }

    private void addEvolutionSection(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.evolution() == null || data.evolution().points() == null
                || data.evolution().points().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("2. Évolution", SECTION_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        addHeaderCell(table, "Période");
        addHeaderCell(table, "Créées");
        addHeaderCell(table, "Terminées");

        data.evolution().points().forEach(point -> {
            addDataCell(table, point.label());
            addDataCell(table, String.valueOf(point.created()));
            addDataCell(table, String.valueOf(point.completed()));
        });

        document.add(table);
    }

    private void addDistributionSection(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.distribution() == null || data.distribution().items() == null
                || data.distribution().items().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("3. Répartition par statut", SECTION_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addHeaderCell(table, "Statut");
        addHeaderCell(table, "Nombre");

        for (DashboardDistributionItemResDto item : data.distribution().items()) {
            addDataCell(table, item.name());
            addDataCell(table, String.valueOf(item.count()));
        }

        addDataCell(table, "Total");
        addDataCell(table, String.valueOf(data.distribution().total()));

        document.add(table);
    }

    private void addActivitySection(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentActivity() == null || data.recentActivity().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("4. Activité récente", SECTION_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        addHeaderCell(table, "Type");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Utilisateur");
        addHeaderCell(table, "Date");

        for (DashboardActivityItemResDto activity : data.recentActivity()) {
            addDataCell(table, activity.type());
            addDataCell(table, truncate(activity.description(), 40));
            addDataCell(table, activity.userName() != null ? activity.userName() : "");
            addDataCell(table,
                    activity.createdAt() != null ? activity.createdAt().toString() : "");
        }

        document.add(table);
    }

    private void addProjectsSection(Document document, DashboardDataResDto data)
            throws DocumentException {
        if (data.recentProjects() == null || data.recentProjects().isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("5. Projets récents", SECTION_FONT);
        sectionTitle.setSpacingBefore(15);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);

        addHeaderCell(table, "Projet");
        addHeaderCell(table, "Propriétaire");
        addHeaderCell(table, "Progression");

        for (DashboardRecentProjectResDto project : data.recentProjects()) {
            addDataCell(table, project.title());
            addDataCell(table, project.ownerName());
            addDataCell(table, project.progress() + "%");
        }

        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer =
                new Paragraph("Rapport généré automatiquement par Collab Task", FOOTER_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }

    private void addSeparator(Document document) throws DocumentException {
        Paragraph separator = new Paragraph("─".repeat(80), BODY_FONT);
        separator.setAlignment(Element.ALIGN_CENTER);
        separator.setSpacingAfter(10);
        document.add(separator);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(75, 85, 99));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String formatPeriod(String period) {
        return switch (period) {
            case "TODAY" -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS" -> "30 derniers jours";
            case "LAST_3_MONTHS" -> "3 derniers mois";
            case "THIS_YEAR" -> "Cette année";
            case "CUSTOM" -> "Période personnalisée";
            default -> period;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
