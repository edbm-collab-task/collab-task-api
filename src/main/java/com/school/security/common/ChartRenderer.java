package com.school.security.common;

import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.school.security.dtos.responses.DashboardEvolutionPointResDto;
import com.school.security.dtos.responses.EvolutionPointResDto;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Rendu des graphiques pour les PDF (barres verticales, courbes).
 * Centralise la logique dupliquée entre les 3 services PDF.
 */
public final class ChartRenderer {
    private ChartRenderer() {}

    public record BarPoint(String label, long created, long completed) {}

    /**
     * Graphe en barres verticales (Créées vs Terminées) avec gestion anti-superposition des abscisses.
     */
    public static Image renderVerticalBarChart(List<BarPoint> points) {
        int scale = 2;
        int imgW = 1000, imgH = 380;
        int left = 60, right = imgW - 20, top = 20, bottom = imgH - 60;
        int plotW = right - left, plotH = bottom - top;
        BufferedImage bi = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);
            long maxC = points.stream().mapToLong(BarPoint::created).max().orElse(1);
            long maxT = points.stream().mapToLong(BarPoint::completed).max().orElse(1);
            long maxL = Math.max(maxC, maxT);
            int max = (int) maxL;
            if (max == 0) max = 1;
            int axisMax = max <= 5 ? 5 : max <= 10 ? 10 : max <= 20 ? 20 : max <= 50 ? 50 : max <= 100 ? 100 : (int) (Math.ceil(max / 50.0) * 50);
            int gridLines = 5;
            java.awt.Font f = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 15);
            g.setFont(f);
            for (int i = 0; i <= gridLines; i++) {
                int y = bottom - (int) ((float) i / gridLines * plotH);
                int v = (int) ((float) i / gridLines * axisMax);
                g.setColor(new Color(230, 230, 230));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(120, 120, 120));
                String lab = String.valueOf(v);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(lab, left - fm.stringWidth(lab) - 8, y + 5);
            }
            g.setColor(new Color(160, 160, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            int n = points.size();
            int groupW = plotW / Math.max(1, n);
            int barW = Math.max(8, groupW / 3);
            int gap = 6;
            for (int i = 0; i < n; i++) {
                BarPoint p = points.get(i);
                int x0 = left + i * groupW + (groupW - barW * 2 - gap) / 2;
                int hC = (int) ((float) p.created() / axisMax * plotH);
                int hT = (int) ((float) p.completed() / axisMax * plotH);
                int yC = bottom - hC;
                int yT = bottom - hT;
                g.setColor(new Color(59, 130, 246));
                g.fillRect(x0, yC, barW, hC);
                g.setColor(new Color(16, 185, 129));
                g.fillRect(x0 + barW + gap, yT, barW, hT);
                if (p.created() > 0) {
                    g.setColor(new Color(59, 130, 246).darker());
                    java.awt.Font sf = new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf);
                    String v = String.valueOf(p.created());
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(v, x0 + barW / 2 - fm.stringWidth(v) / 2, yC - 6);
                }
                if (p.completed() > 0) {
                    g.setColor(new Color(16, 185, 129).darker());
                    java.awt.Font sf = new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf);
                    String v = String.valueOf(p.completed());
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(v, x0 + barW + gap + barW / 2 - fm.stringWidth(v) / 2, yT - 6);
                }
                // abscisses anti-superposition
                g.setFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11));
                g.setColor(new Color(70, 70, 70));
                int stepLabel = n > 20 ? 3 : n > 12 ? 2 : 1;
                if (i % stepLabel == 0 || i == n - 1) {
                    String lab = p.label();
                    if (lab.length() > 10) lab = lab.substring(0, 10);
                    FontMetrics fm = g.getFontMetrics();
                    int lx = left + i * groupW + groupW / 2 - fm.stringWidth(lab) / 2;
                    if (n > 15) {
                        java.awt.geom.AffineTransform old = g.getTransform();
                        g.rotate(Math.toRadians(-30), lx + fm.stringWidth(lab) / 2, bottom + 18);
                        g.drawString(lab, lx, bottom + 18);
                        g.setTransform(old);
                    } else {
                        g.drawString(lab, lx, bottom + 20);
                    }
                }
            }
            g.setColor(new Color(59, 130, 246));
            g.fillRect(right - 180, top - 5, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString("Créées", right - 162, top + 6);
            g.setColor(new Color(16, 185, 129));
            g.fillRect(right - 80, top - 5, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString("Terminées", right - 62, top + 6);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            Image img = Image.getInstance(baos.toByteArray());
            img.scaleToFit(495f, img.getHeight() / 2f);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Calcule un maximum d'axe arrondi pour les line charts. */
    public static int computeNiceMax(int value) {
        if (value <= 5) return 5;
        if (value <= 10) return 10;
        if (value <= 20) return 20;
        if (value <= 50) return 50;
        if (value <= 100) return 100;
        if (value <= 200) return 200;
        int magnitude = (int) Math.pow(10, (int) Math.log10(value));
        int normalized = (int) Math.ceil((double) value / magnitude);
        return normalized * magnitude;
    }

    private static final Color LINE_BLUE = new Color(59, 130, 246);
    private static final Color LINE_GREEN = new Color(16, 185, 129);

    /**
     * Graphe en barres verticales mono-série (projets créés).
     */
    public static Image renderSingleBarChart(String legendLabel, List<EvolutionPointResDto> points) {
        int scale = 2;
        int imgW = 1000, imgH = 380;
        int left = 60, right = imgW - 20, top = 20, bottom = imgH - 60;
        int plotW = right - left, plotH = bottom - top;
        BufferedImage bi = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgW, imgH);
            int max = points.stream().mapToInt(p -> (int) p.created()).max().orElse(1);
            if (max == 0) max = 1;
            int axisMax = max <= 5 ? 5 : max <= 10 ? 10 : max <= 20 ? 20 : max <= 50 ? 50 : max <= 100 ? 100 : (int) (Math.ceil(max / 50.0) * 50);
            int gridLines = 5;
            java.awt.Font f = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 15);
            g.setFont(f);
            for (int i = 0; i <= gridLines; i++) {
                int y = bottom - (int) ((float) i / gridLines * plotH);
                int v = (int) ((float) i / gridLines * axisMax);
                g.setColor(new Color(230, 230, 230));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(120, 120, 120));
                String lab = String.valueOf(v);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(lab, left - fm.stringWidth(lab) - 8, y + 5);
            }
            g.setColor(new Color(160, 160, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);
            int n = points.size();
            int groupW = plotW / Math.max(1, n);
            int barW = Math.max(10, groupW / 2 - 6);
            for (int i = 0; i < n; i++) {
                int x0 = left + i * groupW + (groupW - barW) / 2;
                int val = (int) points.get(i).created();
                int h = (int) ((float) val / axisMax * plotH);
                int y = bottom - h;
                g.setColor(LINE_BLUE);
                g.fillRect(x0, y, barW, h);
                if (val > 0) {
                    g.setColor(LINE_BLUE.darker());
                    java.awt.Font sf = new java.awt.Font("Helvetica", java.awt.Font.BOLD, 13);
                    g.setFont(sf);
                    String v = String.valueOf(val);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(v, x0 + barW / 2 - fm.stringWidth(v) / 2, y - 6);
                }
                g.setFont(new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 11));
                g.setColor(new Color(70, 70, 70));
                int stepLabel = n > 20 ? 3 : n > 12 ? 2 : 1;
                if (i % stepLabel == 0 || i == n - 1) {
                    String lab = points.get(i).label();
                    if (lab.length() > 10) lab = lab.substring(0, 10);
                    FontMetrics fm = g.getFontMetrics();
                    int lx = left + i * groupW + groupW / 2 - fm.stringWidth(lab) / 2;
                    if (n > 15) {
                        java.awt.geom.AffineTransform old = g.getTransform();
                        g.rotate(Math.toRadians(-30), lx + fm.stringWidth(lab) / 2, bottom + 18);
                        g.drawString(lab, lx, bottom + 18);
                        g.setTransform(old);
                    } else {
                        g.drawString(lab, lx, bottom + 20);
                    }
                }
            }
            g.setColor(LINE_BLUE);
            g.fillRect(right - 120, top - 5, 12, 12);
            g.setColor(Color.BLACK);
            g.drawString(legendLabel, right - 102, top + 6);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            Image img = Image.getInstance(baos.toByteArray());
            img.scaleToFit(495f, img.getHeight() / 2f);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Graphe en courbes (Créées vs Terminées) pour l'évolution temporelle.
     */
    public static Image renderLineChart(List<DashboardEvolutionPointResDto> points) {
        int scale = 2;
        int imgWidth = 1000;
        int imgHeight = 330;
        int plotLeft = 55;
        int plotRight = imgWidth - 20;
        int plotTop = 20;
        int plotBottom = imgHeight - 55;
        int plotWidth = plotRight - plotLeft;
        int plotHeight = plotBottom - plotTop;

        BufferedImage bi = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imgWidth, imgHeight);

            int maxCreated = (int) points.stream().mapToLong(DashboardEvolutionPointResDto::created).max().orElse(1);
            int maxCompleted = (int) points.stream().mapToLong(DashboardEvolutionPointResDto::completed).max().orElse(1);
            int maxVal = Math.max(maxCreated, maxCompleted);
            if (maxVal == 0) maxVal = 1;
            int axisMax = computeNiceMax(maxVal);

            g.setColor(new Color(230, 230, 230));
            g.setStroke(new BasicStroke(1f));
            int gridLines = 5;
            java.awt.Font axisFont = new java.awt.Font("Helvetica", java.awt.Font.PLAIN, 15);
            g.setFont(axisFont);
            for (int i = 0; i <= gridLines; i++) {
                int yPos = plotBottom - (int) ((float) i / gridLines * plotHeight);
                int val = (int) ((float) i / gridLines * axisMax);
                g.setColor(new Color(230, 230, 230));
                g.drawLine(plotLeft, yPos, plotRight, yPos);
                g.setColor(new Color(120, 120, 120));
                String label = String.valueOf(val);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, plotLeft - fm.stringWidth(label) - 8, yPos + 5);
            }

            g.setColor(new Color(160, 160, 160));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(plotLeft, plotTop, plotLeft, plotBottom);
            g.drawLine(plotLeft, plotBottom, plotRight, plotBottom);

            int n = points.size();
            int[] xCoords = new int[n];
            int[] yCreated = new int[n];
            int[] yCompleted = new int[n];
            for (int i = 0; i < n; i++) {
                DashboardEvolutionPointResDto p = points.get(i);
                xCoords[i] = n == 1
                        ? plotLeft + plotWidth / 2
                        : (int) (plotLeft + (float) i / (n - 1) * plotWidth);
                yCreated[i] = plotBottom - (int) ((float) p.created() / axisMax * plotHeight);
                yCompleted[i] = plotBottom - (int) ((float) p.completed() / axisMax * plotHeight);
            }

            g.setColor(LINE_BLUE);
            g.setStroke(new BasicStroke(2.5f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolyline(xCoords, yCreated, n);

            g.setColor(LINE_GREEN);
            g.setStroke(new BasicStroke(2.5f * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawPolyline(xCoords, yCompleted, n);

            java.awt.Font smallFont = new java.awt.Font("Helvetica", java.awt.Font.BOLD, 14);
            FontMetrics smallFm = g.getFontMetrics(smallFont);
            for (int i = 0; i < n; i++) {
                g.setColor(LINE_BLUE);
                g.fillOval(xCoords[i] - 5 * scale, yCreated[i] - 5 * scale, 10 * scale, 10 * scale);
                if (points.get(i).created() > 0) {
                    String v = String.valueOf(points.get(i).created());
                    g.setFont(smallFont);
                    g.drawString(v, xCoords[i] - smallFm.stringWidth(v) / 2, yCreated[i] - 12 * scale);
                }
                g.setColor(LINE_GREEN);
                g.fillOval(xCoords[i] - 5 * scale, yCompleted[i] - 5 * scale, 10 * scale, 10 * scale);
                if (points.get(i).completed() > 0) {
                    String v = String.valueOf(points.get(i).completed());
                    g.setFont(smallFont);
                    g.drawString(v, xCoords[i] - smallFm.stringWidth(v) / 2, yCompleted[i] + 24 * scale);
                }
            }

            g.setFont(axisFont);
            g.setColor(new Color(70, 70, 70));
            for (int i = 0; i < n; i++) {
                String label = points.get(i).label();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, xCoords[i] - fm.stringWidth(label) / 2, plotBottom + 22);
            }
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            Image img = Image.getInstance(baos.toByteArray());
            img.scaleToFit(495f, img.getHeight() / 2f);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du rendu du graphique d'évolution", e);
        }
    }
}
