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
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;
import com.school.security.common.ChartRenderer;
import com.school.security.common.PdfConstants;
import com.school.security.common.PdfHelper;
import com.school.security.common.PeriodUtils;
import com.school.security.dtos.responses.*;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.AdminDashboardReportService;
import com.school.security.services.contracts.AdminDashboardService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class AdminDashboardReportServiceImpl implements AdminDashboardReportService {

    private AdminDashboardService adminDashboardService;
    private UserRepository userRepository;
    private com.school.security.repositories.ProjectRepository projectRepository;
    private com.school.security.repositories.TaskRepository taskRepository;

    // Constantes centralisées - voir PdfConstants
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

    @Override
    public byte[] generateReport(Long userId, String period, String startDate, String endDate) {
        var user = userRepository.findById(userId).orElse(null);
        RoleType role = user != null
                ? user.getRoles().stream().findFirst().map(Role::getName).orElse(RoleType.USER)
                : RoleType.USER;
        return generateReport(userId, role, period, startDate, endDate);
    }

    @Override
    public byte[] generateReport(Long userId, RoleType role, String period, String startDate, String endDate) {
        AdminDashboardStatsResDto data =
                adminDashboardService.getAdminDashboardStats(userId, period, startDate, endDate);
        User admin = userRepository.findById(userId).orElse(null);
        String adminName = admin != null ? admin.getFirstname() + " " + admin.getLastname() : "Admin";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 45, 45, 40, 45);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, role, period, startDate, endDate, adminName);
            addContexteProjets(document, data);
            addProjetsEvolutionBarChart(document, data, period, startDate, endDate);
            addProjetsActifsTable(document, data);
            addProjetsInactifsTable(document, data);
            addFooter(document);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    private void addHeader(Document document, RoleType role, String period, String startDate, String endDate, String adminName) throws DocumentException {
        // Logo
        try (java.io.InputStream is = getClass().getResourceAsStream("/static/logoEDBM.png")) {
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(80,80);
                document.add(logo);
            }
        } catch (Exception ignored) {}
        Paragraph title = new Paragraph("rapport de suivi des projet", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        document.add(title);
        // plus de nom projet ni Chef de projet générique, remplacé par admin générateur
        Paragraph adminPara = new Paragraph("Généré par : " + adminName, FONT_ROLE);
        adminPara.setAlignment(Element.ALIGN_CENTER);
        adminPara.setSpacingAfter(4);
        document.add(adminPara);
        String periodLabel = formatPeriodWithDates(period, startDate, endDate);
        Paragraph periodPara = new Paragraph("Période : " + periodLabel, FONT_PERIOD);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        periodPara.setSpacingAfter(8);
        document.add(periodPara);
        document.add(new Paragraph("Généré le " + LocalDateTime.now().format(PDF_GENERATED_FORMAT), FONT_FOOTER));
        document.add(new Paragraph(" ", FONT_FOOTER));
    }

    private String formatPeriodWithDates(String period, String sd, String ed){
        return PeriodUtils.formatPeriodWithDates(period, sd, ed);
    }

    private void addContexteProjets(Document document, AdminDashboardStatsResDto data) throws DocumentException {
        Paragraph sec = new Paragraph("Contexte", FONT_SECTION);
        sec.setSpacingBefore(10);
        sec.setSpacingAfter(6);
        document.add(sec);
        long total = data.totalProjects();
        long actif = data.activeProjects();
        long inactif = Math.max(0, total - actif);
        long pctActif = total>0? (actif*100)/total:0;
        long pctInactif = total>0? (inactif*100)/total:0;
        String txt = "Répartition : " + actif + " actif(s) ("+pctActif+"%), " + inactif + " inactif(s) ("+pctInactif+"%). Total : " + total + " projet(s).";
        Paragraph p = new Paragraph(txt, FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COLOR_DARK));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(8);
        document.add(p);
        // barre horizontale simple actif/inactif
        PdfPTable t = new PdfPTable(new float[]{22f,50f,15f});
        t.setWidthPercentage(100);
        long max = Math.max(actif, Math.max(inactif,1));
        for(int i=0;i<2;i++){
            String lab = i==0?"Actifs":"Inactifs";
            long val = i==0?actif:inactif;
            Color c = i==0? COLOR_EMERALD : COLOR_GRAY;
            PdfPCell lc = new PdfPCell(new Phrase(lab, FONT_CHART_LABEL));
            lc.setBorder(Rectangle.NO_BORDER); lc.setPadding(5); t.addCell(lc);
            PdfPCell bc = buildBarCell(val, max, c); t.addCell(bc);
            PdfPCell vc = new PdfPCell(new Phrase(val+" ("+(i==0?pctActif:pctInactif)+"%)", FONT_CHART_VAL));
            vc.setBorder(Rectangle.NO_BORDER); vc.setPadding(5); t.addCell(vc);
        }
        t.setSpacingAfter(8);
        document.add(t);
    }

    private void addProjetsEvolutionBarChart(Document document, AdminDashboardStatsResDto data, String period, String sd, String ed) throws DocumentException {
        if(data.evolution()==null || data.evolution().isEmpty()) return;
        Paragraph sec = new Paragraph("Évolution des projets ("+formatPeriodWithDates(period, sd, ed)+")", FONT_SECTION);
        sec.setSpacingBefore(10);
        sec.setSpacingAfter(6);
        document.add(sec);
        // Réutilise evolution des tâches comme proxy projets (créés)
        Image chart = renderProjetsVerticalBarChart(data.evolution());
        document.add(chart);
    }

    private Image renderProjetsVerticalBarChart(java.util.List<EvolutionPointResDto> points){
        return ChartRenderer.renderSingleBarChart("Projets créés", points);
    }

    private PdfPCell buildBarCell(long value, long maxVal, Color color) {
        return PdfHelper.buildBarCell(value, maxVal, color);
    }

    private void addProjetsActifsTable(Document document, AdminDashboardStatsResDto data) throws DocumentException {
        java.util.List<ProjectStatsResDto> actifs = data.topProjects().stream().filter(p-> "Actif".equals(p.status())).toList();
        Paragraph sec = new Paragraph("Projets actifs", FONT_SECTION);
        sec.setSpacingBefore(10); sec.setSpacingAfter(6); document.add(sec);
        if(actifs.isEmpty()){ document.add(new Paragraph("Aucun projet actif.", FONT_CHART_LABEL)); return; }
        PdfPTable t = new PdfPTable(new float[]{2.5f,3f,2f,1.5f});
        t.setWidthPercentage(100);
        addTableHeader(t,"Projet","Description / Chef de projet","% Finition","Statut");
        for(ProjectStatsResDto p: actifs){
            addTableRow(t,p.title(), p.ownerName(), p.progressPercent()+"%", p.status());
        }
        t.setSpacingAfter(8); document.add(t);
    }
    private void addProjetsInactifsTable(Document document, AdminDashboardStatsResDto data) throws DocumentException {
        java.util.List<ProjectStatsResDto> inactifs = data.topProjects().stream().filter(p-> !"Actif".equals(p.status())).toList();
        Paragraph sec = new Paragraph("Projets inactifs / archivés", FONT_SECTION);
        sec.setSpacingBefore(10); sec.setSpacingAfter(6); document.add(sec);
        if(inactifs.isEmpty()){ document.add(new Paragraph("Aucun projet inactif.", FONT_CHART_LABEL)); return; }
        PdfPTable t = new PdfPTable(new float[]{2.5f,3f,2f,1.5f});
        t.setWidthPercentage(100);
        addTableHeader(t,"Projet","Description / Chef de projet","% Finition","Statut");
        for(ProjectStatsResDto p: inactifs){
            addTableRow(t,p.title(), p.ownerName(), p.progressPercent()+"%", p.status());
        }
        t.setSpacingAfter(8); document.add(t);
    }

    private void addKpiCards(Document document, AdminDashboardStatsResDto data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1});
        table.setSpacingAfter(16);

        addKpiCell(table, "Utilisateurs", String.valueOf(data.totalUsers()), COLOR_BLUE);
        addKpiCell(table, "Projets actifs", String.valueOf(data.activeProjects()), COLOR_EMERALD);
        addKpiCell(table, "Tâches en retard", String.valueOf(data.overdueTasks()), COLOR_RED);
        addKpiCell(table, "Tâches terminées", String.valueOf(data.completedTasks()), COLOR_AMBER);

        document.add(table);
    }

    private void addKpiCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.setBackgroundColor(COLOR_LIGHT_BG);
        cell.setBorderColorBottom(color);
        cell.setBorderWidthBottom(3);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingBottom(14);

        Paragraph valuePara = new Paragraph(value, FONT_KPI_VALUE);
        valuePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(valuePara);

        Paragraph labelPara = new Paragraph(label, FONT_KPI_LABEL);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        labelPara.setSpacingBefore(4);
        cell.addElement(labelPara);

        table.addCell(cell);
    }

    private void addUsersTable(Document document, List<UserStatsResDto> users) throws DocumentException {
        if (users.isEmpty()) return;

        Paragraph sectionTitle = new Paragraph("Top 10 Utilisateurs", FONT_SECTION);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 1.5f, 1f, 1f, 1f});
        table.setSpacingAfter(12);

        addTableHeader(table, "Utilisateur", "Rôle", "Direction", "Assignées", "Terminées", "En retard");

        for (UserStatsResDto u : users) {
            addTableRow(table,
                    u.firstname() + " " + u.lastname(),
                    u.role(),
                    u.direction(),
                    String.valueOf(u.assignedTasks()),
                    String.valueOf(u.completedTasks()),
                    String.valueOf(u.overdueTasks()));
        }

        document.add(table);
    }

    private void addProjectsTable(Document document, List<ProjectStatsResDto> projects) throws DocumentException {
        if (projects.isEmpty()) return;

        Paragraph sectionTitle = new Paragraph("Top 10 Projets", FONT_SECTION);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 1f, 1f, 1f, 1.5f, 1.5f});
        table.setSpacingAfter(12);

        addTableHeader(table, "Projet", "Propriétaire", "Total", "Terminées", "En retard", "Progression", "Statut");

        for (ProjectStatsResDto p : projects) {
            addTableRow(table,
                    p.title(),
                    p.ownerName(),
                    String.valueOf(p.totalTasks()),
                    String.valueOf(p.completedTasks()),
                    String.valueOf(p.overdueTasks()),
                    p.progressPercent() + "%",
                    p.status());
        }

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        PdfHelper.addTableHeader(table, headers);
    }

    private void addTableRow(PdfPTable table, String... cells) {
        PdfHelper.addTableRow(table, cells);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Rapport généré automatiquement par CollaB Tasks — " + LocalDateTime.now().format(PDF_GENERATED_FORMAT),
                FONT_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);
    }
}