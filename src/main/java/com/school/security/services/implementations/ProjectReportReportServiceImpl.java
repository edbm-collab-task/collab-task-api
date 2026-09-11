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
import com.school.security.common.ChartRenderer;
import com.school.security.common.PdfConstants;
import com.school.security.common.PdfHelper;
import com.school.security.common.PeriodUtils;
import com.school.security.dtos.responses.DashboardDataResDto;
import com.school.security.dtos.responses.DashboardEvolutionPointResDto;
import com.school.security.services.contracts.DashboardService;
import com.school.security.services.contracts.ProjectReportReportService;
import com.school.security.services.contracts.ProjectReportService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

    // Constantes centralisées dans PdfConstants (doc FR)
    private static final Font FONT_TITLE       = PdfConstants.FONT_TITLE;
    private static final Font FONT_SUBTITLE    = PdfConstants.FONT_SUBTITLE;
    private static final Font FONT_SECTION     = PdfConstants.FONT_SECTION;
    private static final Font FONT_TH          = PdfConstants.FONT_TH;
    private static final Font FONT_TD          = PdfConstants.FONT_TD;
    private static final Font FONT_SMALL       = PdfConstants.FONT_SMALL;
    private static final Font FONT_FOOTER      = PdfConstants.FONT_FOOTER;
    private static final DateTimeFormatter PDF_DATE_FORMAT = PdfConstants.PDF_DATE_FORMAT;
    private static final DateTimeFormatter PDF_GENERATED_FORMAT = PdfConstants.PDF_GENERATED_FORMAT;

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
        Paragraph projectName = new Paragraph(data.projectTitle(), PdfConstants.FONT_BOLD_14);
        projectName.setAlignment(Element.ALIGN_CENTER);
        projectName.setSpacingAfter(2);
        document.add(projectName);

        // Chef de projet
        Paragraph chefProjet = new Paragraph("Chef de projet : " + data.projectManagerName(),
                PdfConstants.FONT_BODY);
        chefProjet.setAlignment(Element.ALIGN_CENTER);
        chefProjet.setSpacingAfter(2);
        document.add(chefProjet);

        // Période + date
        Paragraph periodPara = new Paragraph("Période : " + formatPeriodWithDates(period, startDate, endDate),
                PdfConstants.FONT_TD);
        periodPara.setAlignment(Element.ALIGN_CENTER);
        periodPara.setSpacingAfter(2);
        document.add(periodPara);

        Paragraph genPara = new Paragraph("Édité le : " + LocalDateTime.now().format(PDF_GENERATED_FORMAT),
                PdfConstants.FONT_FOOTER);
        genPara.setAlignment(Element.ALIGN_CENTER);
        genPara.setSpacingAfter(10);
        document.add(genPara);

        // Ligne de séparation
        addHorizontalRule(document, PdfConstants.BLACK, 1f);
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
                PdfConstants.FONT_BODY);
        overduePara.setSpacingAfter(6);
        document.add(overduePara);

        addHorizontalRule(document, PdfConstants.BLACK, 1f);
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
                PdfConstants.FONT_BODY);
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
                PdfConstants.FONT_BODY);
        introTextPara.setAlignment(Element.ALIGN_JUSTIFIED);
        introTextPara.setSpacingAfter(12);
        document.add(introTextPara);

        addHorizontalRule(document, PdfConstants.BLACK, 1f);
    }

    private String formatPeriodWithDates(String period, String startDate, String endDate) {
        return PeriodUtils.formatPeriodWithDates(period, startDate, endDate);
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
            addHorizontalRule(document, PdfConstants.BLACK, 1f);
        } catch (Exception e) {
            // silencieux
        }
    }

    private Image renderVerticalBarChart(java.util.List<DashboardEvolutionPointResDto> points) {
        // Délégué à ChartRenderer centralisé (évite duplication)
        java.util.List<ChartRenderer.BarPoint> barPoints = points.stream()
                .map(p -> new ChartRenderer.BarPoint(p.label(), p.created(), p.completed()))
                .toList();
        return ChartRenderer.renderVerticalBarChart(barPoints);
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
                    PdfConstants.FONT_ITALIC_9);
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
                    PdfConstants.FONT_BOLD_12);
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
                PdfConstants.FONT_BODY);
        overduePara.setSpacingAfter(6);
        document.add(overduePara);

        addHorizontalRule(document, PdfConstants.BLACK, 1f);
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
                    PdfConstants.FONT_ITALIC_9);
            noContributors.setAlignment(Element.ALIGN_CENTER);
            noContributors.setSpacingAfter(12);
            document.add(noContributors);
            return;
        }

        // Liste des contributeurs avec des puces (tirets)
        Paragraph contributorsPara = new Paragraph("Contributeurs :", PdfConstants.FONT_BOLD_10);
        contributorsPara.setSpacingAfter(4);
        document.add(contributorsPara);

        for (String contributor : contributors) {
            Paragraph contributorPara = new Paragraph("- " + contributor,
                    PdfConstants.FONT_BODY);
            contributorPara.setSpacingAfter(2);
            document.add(contributorPara);
        }
    }

// ================== FOOTER ==================
    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Rapport généré automatiquement par Collab Task — " + LocalDateTime.now().format(PDF_GENERATED_FORMAT),
                PdfConstants.FONT_FOOTER_DARK);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        document.add(footer);

        Paragraph contact = new Paragraph(
                "EDBM -EDBM Building, Avenue Gal Gabriel, RAMANANTSOA, Antananarivo - edbm.mg",
                PdfConstants.FONT_FOOTER_DARK);
        contact.setAlignment(Element.ALIGN_CENTER);
        contact.setSpacingBefore(4);
        document.add(contact);
    }

// ================== MÉTHODES UTILITAIRES (déléguées à PdfHelper pour DRY) ==================
    private void addHorizontalRule(Document document, Color color, float thickness) throws DocumentException {
        PdfHelper.addHorizontalRule(document, color, thickness);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        PdfHelper.addTableHeaderProject(table, headers);
    }

    private void addTableRow(PdfPTable table, String... cells) {
        PdfHelper.addTableRowProject(table, cells);
    }
}