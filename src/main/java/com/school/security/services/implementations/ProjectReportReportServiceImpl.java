package com.school.security.services.implementations;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.school.security.dtos.responses.ProjectReportResDto;
import com.school.security.dtos.responses.TaskReportResDto;
import com.school.security.entities.Role;
import com.school.security.enums.RoleType;
import com.school.security.repositories.*;
import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.dtos.responses.DashboardEvolutionPointResDto;
import com.school.security.services.contracts.DashboardService;
import com.school.security.services.contracts.ProjectReportReportService;
import com.school.security.services.contracts.ProjectReportService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class ProjectReportReportServiceImpl implements ProjectReportReportService {

    private final ProjectReportService projectReportService;
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final RiskRepository riskRepository;
    private final ActionItemRepository actionItemRepository;

    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color BORDER_GRAY = new Color(200, 200, 200);

    private static final Font FONT_TITLE       = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.BOLD, Color.BLACK);
    private static final Font FONT_SUBTITLE    = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, Color.BLACK);
    private static final Font FONT_SECTION     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, Color.BLACK);
    private static final Font FONT_KPI_LABEL   = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font FONT_KPI_VALUE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, Color.BLACK);
    private static final Font FONT_TH          = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.BLACK);
    private static final Font FONT_TD          = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.BLACK);
    private static final Font FONT_SMALL       = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.BLACK);
    private static final Font FONT_FOOTER      = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.BLACK);
    private static final Font FONT_LEGEND      = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.BLACK);

    private static final DateTimeFormatter PDF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter PDF_GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    @Override
    public byte[] generateReport(Long userId, String period, String startDate, String endDate) {
        var user = userRepository.findById(userId).orElse(null);
        RoleType role = user != null
                ? user.getRoles().stream().findFirst().map(Role::getName).orElse(RoleType.USER)
                : RoleType.USER;
        return generateReport(userId, null, role, period, startDate, endDate);
    }

    @Override
    public byte[] generateReport(Long userId, Long projectId, RoleType role, String period, String startDate, String endDate) {
        ProjectReportResDto data = projectReportService.getProjectReport(userId, projectId);

        if (data == null) {
            throw new IllegalArgumentException("Projet non accessible");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, data, role, period, startDate, endDate);
            addIntroduction(document, data);
            addProjectEvolutionChart(document, userId, projectId, period, startDate, endDate);
            addContributorsList(document, data);
            addTasksTable(document, data);
            addOverdueTasksSection(document, data);
            addFooter(document);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    // ================== HEADER ==================
    private void addHeader(Document document, ProjectReportResDto data, RoleType role, String period, String startDate, String endDate) throws DocumentException, Exception {
        // Logo EDBM - chargement depuis les ressources
        try (InputStream logoStream = getClass().getResourceAsStream("/static/logoEDBM.png")) {
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(80, 80);
                document.add(logo);
            } else {
                System.out.println("Logo non trouvé : /static/logoEDBM.png introuvable dans le classpath");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors du chargement du logo : " + e.getMessage());
        }

        // Titre principal - remplacé
        Paragraph title = new Paragraph("rapport de suivi d'un projet", FONT_TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(2);
        document.add(title);

        // Nom du projet
        Paragraph projectName = new Paragraph(data.projectTitle(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Font.NORMAL, Color.BLACK));
        projectName.setAlignment(Element.ALIGN_CENTER);
        projectName.setSpacingAfter(2);
        document.add(projectName);

        // Chef de projet
        Paragraph chefProjet = new Paragraph("Chef de projet : " + data.projectManagerName(),
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
        chefProjet.setAlignment(Element.ALIGN_CENTER);
        chefProjet.setSpacingAfter(2);
        document.add(chefProjet);

        // Période + date
        Paragraph periodPara = new Paragraph("Période : " + formatPeriodWithDates(period, startDate, endDate),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.BLACK));
        periodPara.setAlignment(Element.ALIGN_CENTER);
        periodPara.setSpacingAfter(2);
        document.add(periodPara);

        Paragraph genPara = new Paragraph("Édité le : " + LocalDateTime.now().format(PDF_GENERATED_FORMAT),
                FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.BLACK));
        genPara.setAlignment(Element.ALIGN_CENTER);
        genPara.setSpacingAfter(10);
        document.add(genPara);

        // Ligne de séparation
        addHorizontalRule(document, Color.BLACK, 1f);
    }

// ================== TÂCHES EN RETARD ==================
    private void addOverdueTasksSection(Document document, ProjectReportResDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("TÂCHES EN RETARD", FONT_SECTION);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        int overdue = data.overdueTasks();
        String overdueText = overdue > 0 ? String.valueOf(overdue) : "vide";

        Paragraph overduePara = new Paragraph(
                "Tâches en retard : " + overdueText,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
        overduePara.setSpacingAfter(6);
        document.add(overduePara);

        addHorizontalRule(document, Color.BLACK, 1f);
    }

// ================== INTRODUCTION ==================
    private void addIntroduction(Document document, ProjectReportResDto data) throws DocumentException {
        Paragraph ctxTitle = new Paragraph("Contexte", FONT_SECTION);
        ctxTitle.setSpacingBefore(10);
        ctxTitle.setSpacingAfter(6);
        document.add(ctxTitle);

        // Description du projet
        String description = data.projectDescription() != null && !data.projectDescription().isEmpty()
                ? data.projectDescription()
                : "Ce projet vise à organiser et suivre l'avancement des tâches au sein de l'équipe.";
        Paragraph descPara = new Paragraph("Description : " + description,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
        descPara.setSpacingAfter(6);
        document.add(descPara);

        // Contexte par statut (pourcentage d'évolution) - saut de ligne avant
        int total = data.totalTasks();
        int completed = data.completedTasks();
        int inProgress = data.inProgressTasks();
        int overdue = data.overdueTasks();
        int todo = data.todoTasks();

        int pctCompleted = total > 0 ? (completed * 100) / total : 0;
        int pctInProgress = total > 0 ? (inProgress * 100) / total : 0;
        int pctTodo = total > 0 ? (todo * 100) / total : 0;
        int pctOverdue = total > 0 ? (overdue * 100) / total : 0;

        StringBuilder introText = new StringBuilder();
        introText.append("Répartition : ").append(completed).append(" terminée(s) (").append(pctCompleted).append("%), ")
                .append(inProgress).append(" en cours(").append(pctInProgress).append("%), ")
                .append(todo).append(" à faire (").append(pctTodo).append("%), ")
                .append(overdue).append(" en retard (").append(pctOverdue).append("%).");

        Paragraph introTextPara = new Paragraph(introText.toString(),
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
        introTextPara.setAlignment(Element.ALIGN_JUSTIFIED);
        introTextPara.setSpacingAfter(12);
        document.add(introTextPara);

        addHorizontalRule(document, Color.BLACK, 1f);
    }

    private String formatPeriodWithDates(String period, String startDate, String endDate) {
        if (period == null) return "";
        return switch (period) {
            case "TODAY" -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS" -> "30 derniers jours";
            case "LAST_3_MONTHS" -> "3 derniers mois";
            case "THIS_YEAR" -> "Cette année";
            case "CUSTOM" -> (startDate != null && endDate != null ? startDate + " au " + endDate : "Période personnalisée");
            default -> period;
        };
    }

    private void addProjectEvolutionChart(Document document, Long userId, Long projectId, String period, String startDate, String endDate) throws DocumentException {
        try {
            DashboardDataResDto dash = dashboardService.getDashboardStats(userId, period, startDate, endDate, projectId);
            if (dash == null || dash.evolution() == null || dash.evolution().points() == null || dash.evolution().points().isEmpty()) return;
            java.util.List<DashboardEvolutionPointResDto> points = dash.evolution().points();
            Paragraph sec = new Paragraph("Évolution des tâches (" + formatPeriodWithDates(period, startDate, endDate) + ")", FONT_SECTION);
            sec.setSpacingBefore(10);
            sec.setSpacingAfter(6);
            document.add(sec);
            Image chart = renderVerticalBarChart(points);
            document.add(chart);
            addHorizontalRule(document, Color.BLACK, 1f);
        } catch (Exception e) {
            // silencieux
        }
    }

    private Image renderVerticalBarChart(java.util.List<DashboardEvolutionPointResDto> points) {
        int scale = 2;
        int imgW = 1000, imgH = 380;
        int left = 60, right = imgW - 20, top = 20, bottom = imgH - 60;
        int plotW = right - left, plotH = bottom - top;
        BufferedImage bi = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0,0,imgW,imgH);
            long maxC = points.stream().mapToLong(DashboardEvolutionPointResDto::created).max().orElse(1);
            long maxT = points.stream().mapToLong(DashboardEvolutionPointResDto::completed).max().orElse(1);
            long maxL = Math.max(maxC, maxT);
            int max = (int)maxL;
            if (max==0) max=1;
            int axisMax = max <=5?5: max<=10?10: max<=20?20: max<=50?50: max<=100?100: (int)(Math.ceil(max/50.0)*50);
            int gridLines=5;
            java.awt.Font f = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 15);
            g.setFont(f);
            for(int i=0;i<=gridLines;i++){
                int y = bottom - (int)((float)i/gridLines*plotH);
                int v = (int)((float)i/gridLines*axisMax);
                g.setColor(new Color(230,230,230));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(120,120,120));
                String lab = String.valueOf(v);
                FontMetrics fm=g.getFontMetrics();
                g.drawString(lab, left - fm.stringWidth(lab)-8, y+5);
            }
            g.setColor(new Color(160,160,160));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            int n=points.size();
            int groupW = plotW / Math.max(1,n);
            int barW = Math.max(8, groupW/3);
            int gap = 6;
            for(int i=0;i<n;i++){
                DashboardEvolutionPointResDto p=points.get(i);
                int x0 = left + i*groupW + (groupW - barW*2 - gap)/2;
                int hC = (int)((float)p.created()/axisMax*plotH);
                int hT = (int)((float)p.completed()/axisMax*plotH);
                int yC = bottom - hC;
                int yT = bottom - hT;
                g.setColor(new Color(59,130,246));
                g.fillRect(x0, yC, barW, hC);
                g.setColor(new Color(16,185,129));
                g.fillRect(x0+barW+gap, yT, barW, hT);
                if(p.created()>0){
                    g.setColor(new Color(59,130,246).darker());
                    java.awt.Font sf=new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf);
                    String v=String.valueOf(p.created());
                    FontMetrics fm=g.getFontMetrics();
                    g.drawString(v, x0+barW/2 - fm.stringWidth(v)/2, yC-6);
                }
                if(p.completed()>0){
                    g.setColor(new Color(16,185,129).darker());
                    java.awt.Font sf=new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf);
                    String v=String.valueOf(p.completed());
                    FontMetrics fm=g.getFontMetrics();
                    g.drawString(v, x0+barW+gap+barW/2 - fm.stringWidth(v)/2, yT-6);
                }
                // abscisses : éviter superposition – police plus petite + affichage 1/n si beaucoup de points
                g.setFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11));
                g.setColor(new Color(70,70,70));
                int stepLabel = n > 20 ? 3 : n > 12 ? 2 : 1;
                if (i % stepLabel == 0 || i == n-1) {
                    String lab=p.label();
                    if(lab.length()>10) lab=lab.substring(0,10);
                    FontMetrics fm=g.getFontMetrics();
                    int lx = left + i*groupW + groupW/2 - fm.stringWidth(lab)/2;
                    // rotation légère si encore serré
                    if(n>15){
                        java.awt.geom.AffineTransform old = g.getTransform();
                        g.rotate(Math.toRadians(-30), lx+fm.stringWidth(lab)/2, bottom+18);
                        g.drawString(lab, lx, bottom+18);
                        g.setTransform(old);
                    } else {
                        g.drawString(lab, lx, bottom+20);
                    }
                }
            }
            // legend
            g.setColor(new Color(59,130,246));
            g.fillRect(right-180, top-5, 12,12);
            g.setColor(Color.BLACK);
            g.drawString("Créées", right-162, top+6);
            g.setColor(new Color(16,185,129));
            g.fillRect(right-80, top-5, 12,12);
            g.setColor(Color.BLACK);
            g.drawString("Terminées", right-62, top+6);
        } finally { g.dispose(); }
        try{
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ImageIO.write(bi,"png",baos);
            Image img=Image.getInstance(baos.toByteArray());
            img.scaleToFit(495f, img.getHeight()/2f);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch(Exception e){ throw new RuntimeException(e); }
    }

// ================== TABLEAU DES TÂCHES PAR STATUT ==================
    private void addTasksTable(Document document, ProjectReportResDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("LISTE DES TÂCHES PAR STATUT", FONT_SECTION);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        // Récupérer la liste des tâches
        java.util.List<TaskReportResDto> tasks = data.tasks();
        if (tasks == null || tasks.isEmpty()) {
            Paragraph noTasks = new Paragraph("Aucune tâche trouvée pour ce projet.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.BLACK));
            noTasks.setAlignment(Element.ALIGN_CENTER);
            noTasks.setSpacingAfter(12);
            document.add(noTasks);
            return;
        }

        // Grouper par statut (ordre d'apparition conservé)
        java.util.Map<String, java.util.List<TaskReportResDto>> tasksByStatus = tasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        TaskReportResDto::status,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        // Pour chaque statut, créer un tableau
        for (java.util.Map.Entry<String, java.util.List<TaskReportResDto>> entry : tasksByStatus.entrySet()) {
            String status = entry.getKey();
            java.util.List<TaskReportResDto> statusTasks = entry.getValue();

            // Titre du statut
            Paragraph statusTitle = new Paragraph("Statut : " + status + " (" + statusTasks.size() + " tâche(s))",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, Color.BLACK));
            statusTitle.setSpacingBefore(10);
            statusTitle.setSpacingAfter(4);
            document.add(statusTitle);

            // Création du tableau : 5 colonnes
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(4);
            table.setSpacingAfter(8);
            table.setWidths(new float[]{5f, 30f, 25f, 15f, 25f});

            // En-têtes
            addTableHeader(table, "N°", "Tâche", "Assigné(s)", "Priorité", "Échéance");

            // Remplir les lignes
            int index = 1;
            for (TaskReportResDto task : statusTasks) {
                String assignees = task.assigneeNames() != null && !task.assigneeNames().isEmpty()
                        ? task.assigneeNames()
                        : "—";
                String priority = task.priority() != null ? task.priority() : "—";
                String dueDate = task.dueDate() != null
                        ? task.dueDate().format(PDF_DATE_FORMAT)
                        : "—";
                addTableRow(table,
                        String.valueOf(index),
                        task.title(),
                        assignees,
                        priority,
                        dueDate
                );
                index++;
            }

            document.add(table);
}
}

// ================== TÂCHES EN RETARD ==================
    private void addOverdueSection(Document document, ProjectReportResDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("TÂCHES EN RETARD", FONT_SECTION);
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        int overdue = data.overdueTasks();
        String overdueText = overdue > 0 ? String.valueOf(overdue) : "vide";

        Paragraph overduePara = new Paragraph(
                "Tâches en retard : " + overdueText,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
        overduePara.setSpacingAfter(6);
        document.add(overduePara);

        addHorizontalRule(document, Color.BLACK, 1f);
    }

// ================== LISTE DES CONTRIBUTEURS ==================
    private void addContributorsList(Document document, ProjectReportResDto data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("LISTE DES CONTRIBUTEURS", FONT_SECTION);
        sectionTitle.setSpacingBefore(12);
        sectionTitle.setSpacingAfter(8);
        document.add(sectionTitle);

        // Récupérer les contributeurs uniques depuis les tâches
        Set<String> contributors = new HashSet<>();
        if (data.tasks() != null) {
            for (TaskReportResDto task : data.tasks()) {
                if (task.assigneeNames() != null && !task.assigneeNames().isEmpty()) {
                    String[] assigneeList = task.assigneeNames().split(",");
                    for (String assignee : assigneeList) {
                        contributors.add(assignee.trim());
                    }
                }
            }
        }

        if (contributors.isEmpty()) {
            Paragraph noContributors = new Paragraph("Aucun contributeur trouvé.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, Color.BLACK));
            noContributors.setAlignment(Element.ALIGN_CENTER);
            noContributors.setSpacingAfter(12);
            document.add(noContributors);
            return;
        }

        // Liste des contributeurs avec des puces (tirets)
        Paragraph contributorsPara = new Paragraph("Contributeurs :", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.NORMAL, Color.BLACK));
        contributorsPara.setSpacingAfter(4);
        document.add(contributorsPara);

        for (String contributor : contributors) {
            Paragraph contributorPara = new Paragraph("- " + contributor,
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.BLACK));
            contributorPara.setSpacingAfter(2);
            document.add(contributorPara);
        }
    }

// ================== FOOTER ==================
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Rapport généré automatiquement par Collab Task — " + LocalDateTime.now().format(PDF_GENERATED_FORMAT),
                FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.BLACK));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);

        Paragraph contact = new Paragraph(
                "EDBM -EDBM Building, Avenue Gal Gabriel, RAMANANTSOA, Antananarivo - edbm.mg",
                FontFactory.getFont(FontFactory.HELVETICA, 7, Font.ITALIC, Color.BLACK));
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(4);
        document.add(contact);
    }

// ================== MÉTHODES UTILITAIRES ==================
    private void addHorizontalRule(Document document, Color color, float thickness) throws DocumentException {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderColorBottom(color);
        cell.setBorderWidthBottom(thickness);
        cell.setPadding(0);
        rule.addCell(cell);
        rule.setSpacingAfter(8);
        document.add(rule);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TH));
            cell.setBackgroundColor(LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_GRAY);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String... cells) {
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, FONT_TD));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorderColor(BORDER_GRAY);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }
}