package com.example.pixelpatrol.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final String REPORT_DIR = System.getProperty("user.home") + "/.pixelpatrol/reports/";

    public ReportService() {
        new java.io.File(REPORT_DIR).mkdirs();
    }

    public static class BatchResult {
        public Long projectId;
        public String projectName;
        public Path stagingImg;
        public Path prodImg;
        public Path diffImg;
        public boolean passed;
        public float diffPercent;
        public String errorMessage;

        // Success Constructor
        public BatchResult(Long id, String name, Path staging, Path prod, Path diff, boolean pass, float percent) {
            this.projectId = id;
            this.projectName = name;
            this.stagingImg = staging;
            this.prodImg = prod;
            this.diffImg = diff;
            this.passed = pass;
            this.diffPercent = percent;
            this.errorMessage = null;
        }

        // Failure Constructor
        public BatchResult(Long id, String name, String error) {
            this.projectId = id;
            this.projectName = name;
            this.errorMessage = error;
            this.passed = false;
            this.diffPercent = 0.0f;
        }
    }

    public String generateBatchPdf(List<BatchResult> results) {
        String filename = "Full_Regression_Report.pdf";
        String fullPath = REPORT_DIR + filename;
        try {
            Document document = new Document(PageSize.A2.rotate());
            document.setMargins(30, 30, 30, 30);
            PdfWriter.getInstance(document, new FileOutputStream(fullPath));
            document.open();

            // 1. Pass the FULL list to create the summary table
            addCoverPage(document, results);

            // 2. Add individual project pages
            for (BatchResult result : results) {
                document.newPage();
                addProjectPage(document, result);
            }
            document.close();
            return filename;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public String generatePdf(String projectName, Path stagingImg, Path prodImg, Path diffImg, boolean passed) {
        String safeName = projectName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String filename = "Report_" + safeName + ".pdf";
        String fullPath = REPORT_DIR + filename;
        try {
            Document document = new Document(PageSize.A2.rotate());
            document.setMargins(30, 30, 30, 30);
            PdfWriter.getInstance(document, new FileOutputStream(fullPath));
            document.open();

            BatchResult result = new BatchResult(0L, projectName, stagingImg, prodImg, diffImg, passed, 0.0f);
            addProjectPage(document, result);

            document.close();
            return filename;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // --- UPDATED COVER PAGE WITH SUMMARY TABLE ---
    private void addCoverPage(Document doc, List<BatchResult> results) throws DocumentException {
        // 1. Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 48);
        Paragraph title = new Paragraph("PixelPatrol Regression Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(50);
        doc.add(title);

        // 2. Date
        Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), FontFactory.getFont(FontFactory.HELVETICA, 24));
        date.setAlignment(Element.ALIGN_CENTER);
        date.setSpacingAfter(50);
        doc.add(date);

        // 3. Summary Table Setup
        // Columns: Project Name (30%) | Status (20%) | Details (50%)
        PdfPTable table = new PdfPTable(new float[]{3f, 2f, 5f});
        table.setWidthPercentage(95);
        table.setSpacingBefore(20);

        // Header Row
        addTableHeader(table, "Project Name");
        addTableHeader(table, "Status");
        addTableHeader(table, "Details / Error Log");

        // Data Rows
        for (BatchResult result : results) {
            // Col 1: Name
            addTableCell(table, result.projectName, Color.BLACK, true);

            // Col 2: Status
            if (result.errorMessage != null) {
                addTableCell(table, "⚠️ ERROR", Color.ORANGE, true);
            } else if (result.passed) {
                addTableCell(table, "✅ PASS", Color.GREEN, true);
            } else {
                String failText = String.format("❌ FAIL (%.2f%%)", result.diffPercent);
                addTableCell(table, failText, Color.RED, true);
            }

            // Col 3: Details
            if (result.errorMessage != null) {
                addTableCell(table, result.errorMessage, Color.RED, false);
            } else {
                addTableCell(table, "Successfully Compared", Color.DARK_GRAY, false);
            }
        }

        doc.add(table);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPadding(12);
        cell.setPhrase(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color color, boolean bold) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        Font font = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 18);
        font.setColor(color);
        cell.setPhrase(new Phrase(text, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    // --- INDIVIDUAL PROJECT PAGES (Kept same as before) ---
    private void addProjectPage(Document doc, BatchResult result) throws DocumentException {
        // ... (Header Section)
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32);
        Paragraph pTitle = new Paragraph("Project: " + result.projectName, headerFont);
        doc.add(pTitle);

        if (result.errorMessage != null) {
            Font errFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            errFont.setColor(Color.RED);
            doc.add(new Paragraph("❌ CRITICAL ERROR: " + result.errorMessage, errFont));
            return;
        }

        Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
        statusFont.setColor(result.passed ? Color.GREEN : Color.RED);
        String statusText = result.passed ? "✅ STATUS: PASSED" : "❌ STATUS: REGRESSION DETECTED";
        if (!result.passed && result.diffPercent > 0) {
            statusText += String.format(" (%.2f%% Difference)", result.diffPercent);
        }
        doc.add(new Paragraph(statusText, statusFont));

        // ... (Image Grid Page 1)
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);

        addCellWithImage(table, "Staging (Original)", result.stagingImg, 600f);
        addCellWithImage(table, "Production (Original)", result.prodImg, 600f);
        doc.add(table);

        // ... (Diff Map Page 2)
        if (!result.passed && result.diffImg != null) {
            doc.newPage();
            Paragraph diffHeader = new Paragraph("Difference Map (Full Resolution Detail)", FontFactory.getFont(FontFactory.HELVETICA, 24));
            diffHeader.setAlignment(Element.ALIGN_CENTER);
            diffHeader.setSpacingAfter(20);
            doc.add(diffHeader);

            try {
                Image img = Image.getInstance(result.diffImg.toString());
                float pageWidth = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin();
                float pageHeight = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin() - 100;
                img.scaleToFit(pageWidth, pageHeight);
                img.setAlignment(Element.ALIGN_CENTER);
                img.setBorder(Rectangle.BOX);
                img.setBorderWidth(1);
                img.setBorderColor(Color.RED);
                doc.add(img);
            } catch (Exception e) {}
        }
    }

    private void addCellWithImage(PdfPTable table, String title, Path imgPath, float maxHeight) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        Paragraph p = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        if (imgPath != null) {
            try {
                Image img = Image.getInstance(imgPath.toString());
                img.scaleToFit(800, maxHeight);
                img.setAlignment(Element.ALIGN_CENTER);
                img.setBorder(Rectangle.BOX);
                img.setBorderWidth(1);
                cell.addElement(img);
            } catch (Exception e) {}
        } else {
            cell.addElement(new Paragraph("Image Unavailable"));
        }
        table.addCell(cell);
    }
}