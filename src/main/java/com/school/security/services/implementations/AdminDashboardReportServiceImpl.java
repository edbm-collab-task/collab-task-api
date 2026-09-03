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
import com.school.security.dtos.responses.*;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.AdminDashboardReportService;
import com.school.security.services.contracts.AdminDashboardService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
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

    private static final Color COLOR_PRIMARY    = new Color(30, 30, 30);
    private static final Color COLOR_DARK       = new Color(30, 30, 30);
    private static final Color COLOR_HEADER_BG  = new Color(240, 240, 240);
    private static final Color COLOR_HEADER_FG  = Color.BLACK;
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

    private static final DateTimeFormatter PDF_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy — HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter PDF_GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
        if(period==null) return "";
        return switch(period){
            case "TODAY" -> "Aujourd'hui";
            case "LAST_7_DAYS" -> "7 derniers jours";
            case "LAST_30_DAYS" -> "30 derniers jours";
            case "LAST_3_MONTHS" -> "3 derniers mois";
            case "THIS_YEAR" -> "Cette année";
            case "CUSTOM" -> (sd!=null&&ed!=null? sd+" au "+ed : "Période personnalisée");
            default -> period;
        };
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
        int scale=2; int imgW=1000, imgH=380; int left=60,right=imgW-20,top=20,bottom=imgH-60;
        int plotW=right-left, plotH=bottom-top;
        BufferedImage bi=new BufferedImage(imgW,imgH,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=bi.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE); g.fillRect(0,0,imgW,imgH);
            int max = points.stream().mapToInt(p-> (int)Math.max(p.created(), p.completed())).max().orElse(1);
            if(max==0) max=1;
            int axisMax = max<=5?5: max<=10?10: max<=20?20: max<=50?50: max<=100?100: (int)(Math.ceil(max/50.0)*50);
            int gridLines=5;
            java.awt.Font f=new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 15);
            g.setFont(f);
            for(int i=0;i<=gridLines;i++){
                int y=bottom-(int)((float)i/gridLines*plotH);
                int v=(int)((float)i/gridLines*axisMax);
                g.setColor(new Color(230,230,230)); g.drawLine(left,y,right,y);
                g.setColor(new Color(120,120,120));
                String lab=String.valueOf(v);
                FontMetrics fm=g.getFontMetrics();
                g.drawString(lab, left-fm.stringWidth(lab)-8, y+5);
            }
            g.setColor(new Color(160,160,160)); g.setStroke(new BasicStroke(2f));
            g.drawLine(left,top,left,bottom); g.drawLine(left,bottom,right,bottom);
            int n=points.size(); int groupW=plotW/Math.max(1,n); int barW=Math.max(10, groupW/2 -6);
            for(int i=0;i<n;i++){
                EvolutionPointResDto p=points.get(i);
                int x0= left + i*groupW + (groupW - barW)/2;
                int h = (int)((float)p.created()/axisMax*plotH);
                int y = bottom - h;
                g.setColor(COLOR_BLUE); g.fillRect(x0,y,barW,h);
                if(p.created()>0){
                    g.setColor(COLOR_BLUE.darker());
                    java.awt.Font sf=new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf); String v=String.valueOf(p.created());
                    FontMetrics fm=g.getFontMetrics();
                    g.drawString(v, x0+barW/2 - fm.stringWidth(v)/2, y-6);
                }
                g.setFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11));
                g.setColor(new Color(70,70,70));
                int stepLabel = n > 20 ? 3 : n > 12 ? 2 : 1;
                if (i % stepLabel == 0 || i == n-1) {
                    String lab=p.label();
                    if(lab.length()>10) lab=lab.substring(0,10);
                    FontMetrics fm=g.getFontMetrics();
                    int lx= left + i*groupW + groupW/2 - fm.stringWidth(lab)/2;
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
            g.setColor(COLOR_BLUE); g.fillRect(right-120, top-5, 12,12); g.setColor(Color.BLACK); g.drawString("Projets créés", right-102, top+6);
        } finally{ g.dispose(); }
        try{ ByteArrayOutputStream baos=new ByteArrayOutputStream(); ImageIO.write(bi,"png",baos); Image img=Image.getInstance(baos.toByteArray()); img.scaleToFit(495f, img.getHeight()/2f); img.setAlignment(Element.ALIGN_CENTER); return img; }catch(Exception e){ throw new RuntimeException(e); }
    }

    private PdfPCell buildBarCell(long value, long maxVal, Color color) {
        float barPct = maxVal > 0 ? (float) value / maxVal : 0;
        float remainingPct = 1f - barPct;
        PdfPTable barWrapper = new PdfPTable(2);
        barWrapper.setWidthPercentage(100);
        try{ barWrapper.setWidths(new float[]{barPct * 100f, remainingPct * 100f + 0.01f}); }catch(Exception ignored){}
        PdfPCell barFiller = new PdfPCell(); barFiller.setBorder(Rectangle.NO_BORDER); barFiller.setBackgroundColor(color); barFiller.setFixedHeight(14); barWrapper.addCell(barFiller);
        PdfPCell emptyFiller = new PdfPCell(); emptyFiller.setBorder(Rectangle.NO_BORDER); emptyFiller.setBackgroundColor(COLOR_BAR_BG); emptyFiller.setFixedHeight(14); barWrapper.addCell(emptyFiller);
        PdfPCell outerCell = new PdfPCell(barWrapper); outerCell.setBorder(Rectangle.NO_BORDER); outerCell.setPadding(4); return outerCell;
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
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TH));
            cell.setBackgroundColor(COLOR_HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String... cells) {
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, FONT_TD));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(COLOR_BORDER);
            cell.setBorderWidth(1);
            cell.setBackgroundColor(COLOR_LIGHT_BG);
            table.addCell(cell);
        }
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