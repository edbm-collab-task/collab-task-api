package com.school.security.common;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.awt.Color;
import java.io.InputStream;

/**
 * Helpers centralisés pour la génération PDF.
 * Mutualise addTableHeader/Row, addHorizontalRule, buildBarCell et chargement logo.
 */
public final class PdfHelper {
    private PdfHelper() {}

    /** Charge le logo EDBM depuis /static/logoEDBM.png (classpath). */
    public static void addLogo(Document document) throws DocumentException {
        try (InputStream is = PdfHelper.class.getResourceAsStream("/static/logoEDBM.png")) {
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(80, 80);
                document.add(logo);
            }
        } catch (Exception ignored) {}
    }

    public static void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, PdfConstants.FONT_TH));
            cell.setBackgroundColor(PdfConstants.COLOR_HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            cell.setBorderColor(PdfConstants.COLOR_BORDER);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    // Variante projet (gris clair, padding 6)
    public static void addTableHeaderProject(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, PdfConstants.FONT_TH));
            cell.setBackgroundColor(PdfConstants.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            cell.setBorderColor(PdfConstants.BORDER_GRAY);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    public static void addTableRow(PdfPTable table, String... cells) {
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, PdfConstants.FONT_TD));
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(PdfConstants.COLOR_BORDER);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    public static void addTableRowProject(PdfPTable table, String... cells) {
        for (String c : cells) {
            PdfPCell cell = new PdfPCell(new Phrase(c, PdfConstants.FONT_TD));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorderColor(PdfConstants.BORDER_GRAY);
            cell.setBorderWidth(1);
            table.addCell(cell);
        }
    }

    public static void addHorizontalRule(Document document, Color color, float thickness) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderColorBottom(color);
        cell.setBorderWidthBottom(thickness);
        cell.setPadding(0);
        line.addCell(cell);
        line.setSpacingAfter(8);
        document.add(line);
    }

    public static PdfPCell buildBarCell(long value, long maxVal, Color color) {
        float barPct = maxVal > 0 ? (float) value / maxVal : 0;
        float remainingPct = 1f - barPct;
        PdfPTable barWrapper = new PdfPTable(2);
        barWrapper.setWidthPercentage(100);
        try { barWrapper.setWidths(new float[]{barPct * 100f, remainingPct * 100f + 0.01f}); } catch (Exception ignored) {}
        PdfPCell barFiller = new PdfPCell();
        barFiller.setBorder(Rectangle.NO_BORDER);
        barFiller.setBackgroundColor(color);
        barFiller.setFixedHeight(14);
        barWrapper.addCell(barFiller);
        PdfPCell emptyFiller = new PdfPCell();
        emptyFiller.setBorder(Rectangle.NO_BORDER);
        emptyFiller.setBackgroundColor(PdfConstants.COLOR_BAR_BG);
        emptyFiller.setFixedHeight(14);
        barWrapper.addCell(emptyFiller);
        PdfPCell outerCell = new PdfPCell(barWrapper);
        outerCell.setBorder(Rectangle.NO_BORDER);
        outerCell.setPadding(4);
        return outerCell;
    }
}
