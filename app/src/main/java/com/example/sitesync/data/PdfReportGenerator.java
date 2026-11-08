package com.example.sitesync.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.sitesync.models.DailySiteReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utility to generate professional A4 PDF reports from DailySiteReport models
 * using Android's native PdfDocument engine and share them directly with Project Managers.
 */
public class PdfReportGenerator {

    public static File generateReportPdf(Context context, DailySiteReport report) throws IOException {
        int pageWidth = 595; // A4 width at 72 DPI
        int pageHeight = 842; // A4 height at 72 DPI

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pageWidth, pageHeight, bgPaint);

        // Header Background Banner
        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.parseColor("#1A2530")); // Brand Primary
        canvas.drawRect(0, 0, pageWidth, 95, headerPaint);

        // Header Text
        Paint headerTextPaint = new Paint();
        headerTextPaint.setColor(Color.parseColor("#F59E0B")); // Brand Secondary (Construction Gold)
        headerTextPaint.setTextSize(20);
        headerTextPaint.setFakeBoldText(true);
        headerTextPaint.setAntiAlias(true);
        canvas.drawText("SiteSync Construction Intelligence", 36, 40, headerTextPaint);

        Paint subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.WHITE);
        subHeaderPaint.setTextSize(13);
        subHeaderPaint.setAntiAlias(true);
        canvas.drawText("DAILY SITE PROGRESS & RESOURCE REPORT", 36, 62, subHeaderPaint);

        // Report ID & Date in Header
        Paint metaPaint = new Paint();
        metaPaint.setColor(Color.parseColor("#94A3B8"));
        metaPaint.setTextSize(10);
        metaPaint.setTextAlign(Paint.Align.RIGHT);
        metaPaint.setAntiAlias(true);
        canvas.drawText("Report ID: " + report.getId(), pageWidth - 36, 40, metaPaint);
        canvas.drawText("Date: " + report.getReportDate(), pageWidth - 36, 58, metaPaint);
        canvas.drawText("Status: " + report.getStatus().toUpperCase(), pageWidth - 36, 76, metaPaint);

        int currentY = 120;
        int leftMargin = 36;
        int rightMargin = pageWidth - 36;
        int contentWidth = rightMargin - leftMargin;

        // Project Overview Card Background
        Paint cardBgPaint = new Paint();
        cardBgPaint.setColor(Color.parseColor("#F8FAFC"));
        Paint cardStrokePaint = new Paint();
        cardStrokePaint.setColor(Color.parseColor("#E2E8F0"));
        cardStrokePaint.setStyle(Paint.Style.STROKE);
        cardStrokePaint.setStrokeWidth(1);

        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 65, cardBgPaint);
        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 65, cardStrokePaint);

        // Overview Text
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#64748B"));
        labelPaint.setTextSize(9);
        labelPaint.setAntiAlias(true);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#0F172A"));
        valuePaint.setTextSize(11);
        valuePaint.setFakeBoldText(true);
        valuePaint.setAntiAlias(true);

        // Project & Prepared By
        canvas.drawText("PROJECT NAME", leftMargin + 12, currentY + 20, labelPaint);
        canvas.drawText(report.getProjectName(), leftMargin + 12, currentY + 36, valuePaint);

        canvas.drawText("PREPARED BY (SITE ENGINEER)", leftMargin + 250, currentY + 20, labelPaint);
        canvas.drawText(report.getPreparedBy(), leftMargin + 250, currentY + 36, valuePaint);

        // Weather
        canvas.drawText("WEATHER CONDITIONS", leftMargin + 12, currentY + 52, labelPaint);
        canvas.drawText(report.getWeather(), leftMargin + 115, currentY + 52, valuePaint);

        currentY += 85;

        // Section Helper
        TextPaint bodyTextPaint = new TextPaint();
        bodyTextPaint.setColor(Color.parseColor("#1E293B"));
        bodyTextPaint.setTextSize(9.5f);
        bodyTextPaint.setAntiAlias(true);

        currentY = drawSection(canvas, "1. WORK COMPLETED TODAY", report.getCompletedWork(), leftMargin, currentY, contentWidth, bodyTextPaint);
        currentY = drawSection(canvas, "2. MANPOWER DEPLOYMENT", report.getManpower(), leftMargin, currentY, contentWidth, bodyTextPaint);
        currentY = drawSection(canvas, "3. MATERIALS USED", report.getMaterialsUsed(), leftMargin, currentY, contentWidth, bodyTextPaint);
        currentY = drawSection(canvas, "4. EQUIPMENT & MACHINERY DEPLOYED", report.getEquipment(), leftMargin, currentY, contentWidth, bodyTextPaint);
        currentY = drawSection(canvas, "5. SITE PROBLEMS, DELAYS & HINDRANCES", report.getSiteProblems(), leftMargin, currentY, contentWidth, bodyTextPaint);
        currentY = drawSection(canvas, "6. NEXT-DAY SCHEDULE & ACTIONS", report.getNextDayPlans(), leftMargin, currentY, contentWidth, bodyTextPaint);

        // Sign-off / Footer
        Paint footerLinePaint = new Paint();
        footerLinePaint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawLine(leftMargin, pageHeight - 65, rightMargin, pageHeight - 65, footerLinePaint);

        Paint signPaint = new Paint();
        signPaint.setColor(Color.parseColor("#64748B"));
        signPaint.setTextSize(8.5f);
        signPaint.setAntiAlias(true);
        canvas.drawText("SiteSync Quality Management • Verified by Field Engineering Team", leftMargin, pageHeight - 45, signPaint);
        canvas.drawText("Project Manager Copy • Transmission Date: " + report.getReportDate(), leftMargin, pageHeight - 32, signPaint);

        Paint signBoxPaint = new Paint();
        signBoxPaint.setColor(Color.parseColor("#475569"));
        signBoxPaint.setTextSize(9f);
        signBoxPaint.setTextAlign(Paint.Align.RIGHT);
        signBoxPaint.setAntiAlias(true);
        canvas.drawText("Engineer Sign: " + report.getPreparedBy(), rightMargin, pageHeight - 45, signBoxPaint);
        canvas.drawText("Digital Authorization Verified", rightMargin, pageHeight - 32, signBoxPaint);

        pdfDocument.finishPage(page);

        // Save to cache directory
        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File pdfFile = new File(dir, report.getId() + ".pdf");
        FileOutputStream fos = new FileOutputStream(pdfFile);
        pdfDocument.writeTo(fos);
        fos.close();
        pdfDocument.close();

        return pdfFile;
    }

    private static int drawSection(Canvas canvas, String title, String content, int x, int y, int width, TextPaint textPaint) {
        // Section Title
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#1E3A8A")); // Deep Blue Accent
        titlePaint.setTextSize(10.5f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);
        canvas.drawText(title, x, y, titlePaint);
        y += 6;

        // Content
        String displayContent = (content != null && !content.trim().isEmpty()) ? content.trim() : "None recorded.";
        StaticLayout staticLayout = StaticLayout.Builder.obtain(displayContent, 0, displayContent.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2.0f, 1.0f)
                .setIncludePad(false)
                .build();

        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();

        y += staticLayout.getHeight() + 14;
        return y;
    }

    public static void shareReportPdf(Context context, DailySiteReport report) {
        try {
            File pdfFile = generateReportPdf(context, report);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Daily Site Report [" + report.getId() + "] - " + report.getProjectName());
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Dear Project Manager,\n\n" +
                    "Please find attached the Daily Site Report for " + report.getProjectName() + " (" + report.getReportDate() + ").\n\n" +
                    "Summary of Completed Work:\n" + report.getCompletedWork() + "\n\n" +
                    "Resources Deployed:\n" +
                    "• Manpower: " + report.getManpower() + "\n" +
                    "• Materials: " + report.getMaterialsUsed() + "\n" +
                    "• Equipment: " + report.getEquipment() + "\n\n" +
                    "Problems / Issues:\n" + report.getSiteProblems() + "\n\n" +
                    "Next Day Schedule:\n" + report.getNextDayPlans() + "\n\n" +
                    "Submitted by: " + report.getPreparedBy()
            );
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share Daily Report with Project Manager"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
