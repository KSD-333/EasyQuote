package com.example.quotationcreator.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;

import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.model.Customer;
import com.example.quotationcreator.model.Item;
import com.example.quotationcreator.model.Quote;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class QuoteRenderEngine {

    public static final float A4_PORTRAIT_WIDTH = 595f;
    public static final float A4_PORTRAIT_HEIGHT = 842f;
    public static final float A4_WIDTH = A4_PORTRAIT_WIDTH;
    public static final float A4_HEIGHT = A4_PORTRAIT_HEIGHT;

    private static final float PAGE_MARGIN = 32f;
    private static final float TABLE_HEADER_HEIGHT = 26f;
    private static final float ROW_HEIGHT = 27f;
    private static final float FIRST_PAGE_TOP_SECTION = 164f;
    private static final float CONTINUATION_TOP_SECTION = 72f;
    private static final float FOOTER_RESERVE = 264f;

    private QuoteRenderEngine() {
    }

    public static List<PageSlice> paginate(Quote quote) {
        List<PageSlice> pages = new ArrayList<>();
        List<Item> items = quote.getItems();
        float pageHeight = pageHeight(quote);

        if (items == null || items.isEmpty()) {
            pages.add(new PageSlice(0, 0, true, true));
            return pages;
        }

        int cursor = 0;
        boolean firstPage = true;
        while (cursor < items.size()) {
            int rowsPerPage = rowsPerPage(firstPage, pageHeight);
            int end = Math.min(items.size(), cursor + rowsPerPage);
            boolean lastPage = end >= items.size();
            pages.add(new PageSlice(cursor, end, firstPage, lastPage));
            cursor = end;
            firstPage = false;
        }
        return pages;
    }

    public static float pageWidth(Quote quote) {
        return quote != null && quote.isLandscapeMode() ? A4_PORTRAIT_HEIGHT : A4_PORTRAIT_WIDTH;
    }

    public static float pageHeight(Quote quote) {
        return quote != null && quote.isLandscapeMode() ? A4_PORTRAIT_WIDTH : A4_PORTRAIT_HEIGHT;
    }

    private static int rowsPerPage(boolean firstPage, float pageHeight) {
        float topSection = firstPage ? FIRST_PAGE_TOP_SECTION : CONTINUATION_TOP_SECTION;
        float tableStart = PAGE_MARGIN + topSection + 12f + TABLE_HEADER_HEIGHT;
        float availableHeight = (pageHeight - PAGE_MARGIN - FOOTER_RESERVE) - tableStart;
        return Math.max(1, (int) Math.floor(availableHeight / ROW_HEIGHT));
    }

    public static void drawPage(
            Canvas canvas,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile companyProfile,
            PageSlice slice,
            int pageNumber,
            int totalPages) {
        drawPage(canvas, quote, summary, companyProfile, null, null, null, slice, pageNumber, totalPages);
    }

    public static void drawPage(
            Canvas canvas,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile companyProfile,
            Bitmap watermarkLogoBitmap,
            PageSlice slice,
            int pageNumber,
            int totalPages) {
        drawPage(canvas, quote, summary, companyProfile, watermarkLogoBitmap, null, null, slice, pageNumber,
                totalPages);
    }

    public static void drawPage(
            Canvas canvas,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile companyProfile,
            Bitmap watermarkLogoBitmap,
            Bitmap companyLogoBitmap,
            Bitmap signatureBitmap,
            PageSlice slice,
            int pageNumber,
            int totalPages) {
        if (companyProfile == null) {
            companyProfile = new CompanyProfile();
        }

        QuoteTemplateStyle style = QuoteTemplateStyle.fromTemplate(quote.getTemplateType());
        canvas.drawColor(Color.WHITE);

        // Define semi-transparent versions of style colors for background fills
        int headerFillColor = (style.tableHeaderBackground & 0x00FFFFFF) | 0xCC000000; // 80% opacity
        int highlightFillColor = (style.totalHighlight & 0x00FFFFFF) | 0xCC000000; // 80% opacity

        float pageWidth = pageWidth(quote);
        float pageHeight = pageHeight(quote);

        drawWatermark(canvas, quote, companyProfile, watermarkLogoBitmap, pageWidth, pageHeight, style);

        RectF content = new RectF(
                PAGE_MARGIN,
                PAGE_MARGIN,
                pageWidth - PAGE_MARGIN,
                pageHeight - PAGE_MARGIN);

        float y = content.top;
        if (slice.isFirstPage()) {
            y = drawFirstPageHeader(canvas, quote, companyProfile, companyLogoBitmap, content, y, style);
        } else {
            y = drawContinuationHeader(canvas, quote, content, y, style);
        }

        boolean hsnVisible = false;
        for (Item item : quote.getItems()) {
            if (!TextUtils.isEmpty(item.getHsnSacCode())) {
                hsnVisible = true;
                break;
            }
        }

        float tableTop = y + 8f;
        float rowTop = drawTableHeader(canvas, content.left, content.right, tableTop, style, hsnVisible);

        if (quote.getItems().isEmpty()) {
            drawEmptyItemRow(canvas, content.left, content.right, rowTop, style);
            rowTop += ROW_HEIGHT;
        } else {
            for (int index = slice.getItemStart(); index < slice.getItemEnd(); index++) {
                drawItemRow(canvas, quote.getItems().get(index), index + 1, content.left, content.right, rowTop, style,
                        hsnVisible);
                rowTop += ROW_HEIGHT;
            }
        }

        float sectionY = rowTop + 12f;
        if (slice.isLastPage()) {
            drawFinalSection(canvas, quote, summary, companyProfile, signatureBitmap, content, sectionY, style);
        } else {
            drawContinuationNote(canvas, content, sectionY, style);
        }

        drawPageFooter(canvas, pageNumber, totalPages, content, style);
    }

    private static float drawFirstPageHeader(
            Canvas canvas,
            Quote quote,
            CompanyProfile profile,
            Bitmap companyLogoBitmap,
            RectF content,
            float y,
            QuoteTemplateStyle style) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(style.tableBorder);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(17f);
        String title = safe(quote.getTitle(), "Estimate");
        float titleWidth = textPaint.measureText(title);
        canvas.drawText(title, content.centerX() - (titleWidth / 2f), y + 20f, textPaint);

        float companyTop = y + 34f;
        float companyHeight = 64f;
        RectF companyRect = new RectF(content.left, companyTop, content.right, companyTop + companyHeight);
        canvas.drawRect(companyRect, paint);

        float logoRight = companyRect.left + 74f;
        canvas.drawLine(logoRight, companyRect.top, logoRight, companyRect.bottom, paint);

        RectF logoRect = new RectF(companyRect.left + 8f, companyRect.top + 5f, logoRight - 8f,
                companyRect.bottom - 5f);
        if (companyLogoBitmap != null
                && !companyLogoBitmap.isRecycled()
                && companyLogoBitmap.getWidth() > 0
                && companyLogoBitmap.getHeight() > 0) {
            float width = companyLogoBitmap.getWidth();
            float height = companyLogoBitmap.getHeight();
            float scale = Math.min(logoRect.width() / width, logoRect.height() / height);

            float renderWidth = width * scale;
            float renderHeight = height * scale;
            RectF target = new RectF(
                    logoRect.centerX() - (renderWidth / 2f),
                    logoRect.centerY() - (renderHeight / 2f),
                    logoRect.centerX() + (renderWidth / 2f),
                    logoRect.centerY() + (renderHeight / 2f));

            Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            logoPaint.setFilterBitmap(true);
            canvas.drawBitmap(companyLogoBitmap, null, target, logoPaint);
        } else {
            Paint logoFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            logoFill.setStyle(Paint.Style.FILL);
            logoFill.setColor(style.tableHeaderBackground);

            float cx = logoRect.centerX();
            float cy = logoRect.centerY();
            canvas.drawCircle(cx, cy, Math.min(logoRect.width(), logoRect.height()) * 0.38f, logoFill);

            textPaint.setColor(style.accent);
            textPaint.setTextSize(16f);
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            String safeName = safe(profile.getCompanyName(), "Y").trim();
            if (safeName.isEmpty()) {
                safeName = "Y";
            }
            String initial = safeName.substring(0, 1).toUpperCase(Locale.getDefault());
            float initialWidth = textPaint.measureText(initial);
            canvas.drawText(initial, cx - (initialWidth / 2f), cy + 5f, textPaint);
        }

        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setTextSize(9.5f);
        drawTextWithinWidth(canvas, safe(profile.getCompanyName(), "Company Name"), logoRight + 8f,
                companyRect.top + 14f, textPaint, 250f);

        textPaint.setTextSize(8.8f);
        List<String> addrLines = wrapText(safe(profile.getCompanyAddress(), "Address"), textPaint, 250f);
        float currentY = companyRect.top + 25f;
        for (int i = 0; i < addrLines.size() && i < 2; i++) {
            canvas.drawText(addrLines.get(i), logoRight + 8f, currentY, textPaint);
            currentY += 11f;
        }

        drawTextWithinWidth(canvas, "Phone: " + safe(profile.getCompanyPhone(), "-"), logoRight + 8f, currentY + 1f,
                textPaint, 250f);
        drawTextWithinWidth(canvas, "GSTIN: " + safe(profile.getCompanyGstNumber(), "-"), logoRight + 8f,
                currentY + 11f, textPaint, 250f);

        textPaint.setTextSize(8.6f);
        float rightInfoX = companyRect.right - 170f;
        drawTextWithinWidth(canvas, "Email: " + safe(profile.getCompanyEmail(), "-"), rightInfoX, companyRect.top + 25f,
                textPaint, 165f);
        drawTextWithinWidth(canvas, "State: " + safe(profile.getCompanyState(), "-"), rightInfoX, companyRect.top + 37f,
                textPaint, 165f);

        float splitTop = companyRect.bottom;
        float splitHeight = 58f;
        RectF splitRect = new RectF(content.left, splitTop, content.right, splitTop + splitHeight);
        canvas.drawRect(splitRect, paint);

        float splitX = splitRect.left + (splitRect.width() * 0.5f);
        canvas.drawLine(splitX, splitRect.top, splitX, splitRect.bottom, paint);

        float splitMidY = splitRect.top + 20f;
        canvas.drawLine(splitRect.left, splitMidY, splitRect.right, splitMidY, paint);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(10.5f);
        canvas.drawText("Estimate For:", splitRect.left + 6f, splitRect.top + 14f, textPaint);
        canvas.drawText("Estimate Details:", splitX + 6f, splitRect.top + 14f, textPaint);

        Customer customer = quote.getCustomer();
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(11f);
        drawTextWithinWidth(canvas, safe(customer.getName(), "Customer Name"), splitRect.left + 6f, splitRect.top + 33f,
                textPaint, splitX - splitRect.left - 10f);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setTextSize(9f);
        List<String> custAddrLines = wrapText(safe(customer.getAddress(), ""), textPaint,
                splitX - splitRect.left - 12f);
        float custAddrY = splitRect.top + 44f;
        for (int i = 0; i < custAddrLines.size() && i < 2; i++) {
            canvas.drawText(custAddrLines.get(i), splitRect.left + 6f, custAddrY, textPaint);
            custAddrY += 10f;
        }

        textPaint.setTextSize(9.5f);
        drawTextWithinWidth(canvas, "No: " + safe(quote.getQuotationNumber(), "NA"), splitX + 6f, splitRect.top + 34f,
                textPaint, splitRect.right - splitX - 8f);
        drawTextWithinWidth(canvas, "Date: " + formatDate(quote.getDateMillis()), splitX + 6f, splitRect.top + 46f,
                textPaint, splitRect.right - splitX - 8f);

        return splitRect.bottom;
    }

    private static float drawContinuationHeader(Canvas canvas, Quote quote, RectF content, float y,
            QuoteTemplateStyle style) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(style.tableHeaderBackground);

        RectF continuationRect = new RectF(content.left, y, content.right, y + 44f);
        canvas.drawRect(continuationRect, paint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(14f);
        canvas.drawText("Estimate (Continued)", continuationRect.left + 12f, continuationRect.top + 28f, textPaint);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setTextSize(10f);
        String reference = "Ref: " + safe(quote.getQuotationNumber(), "NA");
        float width = textPaint.measureText(reference);
        canvas.drawText(reference, continuationRect.right - width - 12f, continuationRect.top + 28f, textPaint);
        return continuationRect.bottom;
    }

    private static float drawTableHeader(Canvas canvas, float left, float right, float top, QuoteTemplateStyle style,
            boolean hsnVisible) {
        float[] columns = buildColumns(left, right, hsnVisible);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(style.tableHeaderBackground);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1f);
        stroke.setColor(style.tableBorder);

        RectF headerRect = new RectF(left, top, right, top + TABLE_HEADER_HEIGHT);

        // Use semi-transparent fill so watermark shows through
        int alphaFill = (style.tableHeaderBackground & 0x00FFFFFF) | 0xCC000000;
        fill.setColor(alphaFill);
        canvas.drawRect(headerRect, fill);
        canvas.drawRect(headerRect, stroke);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(9.2f);

        String[] labels;
        if (hsnVisible) {
            labels = new String[] { "Sr.No.", "Item & Description", "HSN/SAC", "QTY", "Unit", "Rate", "Total" };
        } else {
            labels = new String[] { "Sr.No.", "Item & Description", "QTY", "Unit", "Rate", "Total" };
        }

        for (int i = 0; i < labels.length; i++) {
            float cellLeft = columns[i];
            float cellRight = columns[i + 1];
            drawCenteredText(canvas, labels[i], cellLeft, cellRight, top, TABLE_HEADER_HEIGHT, textPaint);
            canvas.drawLine(cellRight, top, cellRight, top + TABLE_HEADER_HEIGHT, stroke);
        }

        return top + TABLE_HEADER_HEIGHT;
    }

    private static void drawItemRow(Canvas canvas, Item item, int rowNumber, float left, float right, float top,
            QuoteTemplateStyle style, boolean hsnVisible) {
        float[] columns = buildColumns(left, right, hsnVisible);
        float bottom = top + ROW_HEIGHT;

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(style.tableBorder);
        stroke.setStrokeWidth(0.9f);

        canvas.drawRect(left, top, right, bottom, stroke);
        for (int i = 1; i < columns.length - 1; i++) {
            canvas.drawLine(columns[i], top, columns[i], bottom, stroke);
        }

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textPrimary);
        textPaint.setTextSize(9.4f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        drawCenteredText(canvas, String.valueOf(rowNumber), columns[0], columns[1], top, ROW_HEIGHT, textPaint);

        if (hsnVisible) {
            drawLeftText(canvas, ellipsize(textPaint, safe(item.getItemName(), "-"), columns[2] - columns[1] - 8f),
                    columns[1] + 4f, top, ROW_HEIGHT, textPaint);
            drawCenteredText(canvas, ellipsize(textPaint, safe(item.getHsnSacCode(), ""), columns[3] - columns[2] - 8f),
                    columns[2], columns[3], top, ROW_HEIGHT, textPaint);

            drawCenteredText(canvas, formatNumber(item.getQuantity()), columns[3], columns[4], top, ROW_HEIGHT,
                    textPaint);
            drawCenteredText(canvas, safe(item.getUnit(), "-"), columns[4], columns[5], top, ROW_HEIGHT, textPaint);
            drawRightText(canvas, CurrencyUtils.format(item.getUnitPrice()), columns[6] - 4f, top, ROW_HEIGHT,
                    textPaint);
            drawRightText(canvas, CurrencyUtils.format(item.getLineAmount()), columns[7] - 4f, top, ROW_HEIGHT,
                    textPaint);
        } else {
            drawLeftText(canvas, ellipsize(textPaint, safe(item.getItemName(), "-"), columns[2] - columns[1] - 8f),
                    columns[1] + 4f, top, ROW_HEIGHT, textPaint);

            drawCenteredText(canvas, formatNumber(item.getQuantity()), columns[2], columns[3], top, ROW_HEIGHT,
                    textPaint);
            drawCenteredText(canvas, safe(item.getUnit(), "-"), columns[3], columns[4], top, ROW_HEIGHT, textPaint);
            drawRightText(canvas, CurrencyUtils.format(item.getUnitPrice()), columns[5] - 4f, top, ROW_HEIGHT,
                    textPaint);
            drawRightText(canvas, CurrencyUtils.format(item.getLineAmount()), columns[6] - 4f, top, ROW_HEIGHT,
                    textPaint);
        }
    }

    private static void drawEmptyItemRow(Canvas canvas, float left, float right, float top, QuoteTemplateStyle style) {
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(style.tableBorder);
        stroke.setStrokeWidth(0.9f);
        canvas.drawRect(left, top, right, top + ROW_HEIGHT, stroke);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textMuted);
        textPaint.setTextSize(10f);
        drawCenteredText(canvas, "No items added", left, right, top, ROW_HEIGHT, textPaint);
    }

    private static void drawFinalSection(
            Canvas canvas,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile,
            Bitmap signatureBitmap,
            RectF content,
            float top,
            QuoteTemplateStyle style) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Subtotal", CurrencyUtils.format(summary.getSubtotal())));
        rows.add(new Row("Grand Total", CurrencyUtils.format(summary.getFinalTotal())));

        float totalsWidth = 145f;
        float rowHeight = 19f;
        float totalsHeight = 20f + (rows.size() * rowHeight) + 34f;
        RectF totalsRect = new RectF(content.right - totalsWidth, top, content.right, top + totalsHeight);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.WHITE);
        canvas.drawRect(totalsRect, fill);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1f);
        stroke.setColor(style.tableBorder);
        canvas.drawRect(totalsRect, stroke);

        // Header of totals
        int alphaHeaderFill = (style.tableHeaderBackground & 0x00FFFFFF) | 0xCC000000;
        fill.setColor(alphaHeaderFill);
        canvas.drawRect(totalsRect.left, totalsRect.top, totalsRect.right, totalsRect.top + 20f, fill);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(9f);
        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        canvas.drawText("Totals", totalsRect.left + 6f, totalsRect.top + 14f, textPaint);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowTop = totalsRect.top + 20f + (i * rowHeight);
            boolean isGrandTotal = i == rows.size() - 1;

            if (isGrandTotal) {
                int alphaHighlight = (style.totalHighlight & 0x00FFFFFF) | 0xCC000000;
                fill.setColor(alphaHighlight);
                canvas.drawRect(totalsRect.left, rowTop, totalsRect.right, rowTop + rowHeight, fill);
                textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
                textPaint.setTextSize(10f);
            } else {
                textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
                textPaint.setTextSize(9f);
            }

            textPaint.setColor(style.textPrimary);
            drawLeftText(canvas, row.label, totalsRect.left + 6f, rowTop, rowHeight, textPaint);
            drawRightText(canvas, row.value, totalsRect.right - 5f, rowTop, rowHeight, textPaint);

            canvas.drawLine(totalsRect.left, rowTop + rowHeight, totalsRect.right, rowTop + rowHeight, stroke);
        }

        float wordsTop = totalsRect.bottom - 30f;
        textPaint.setTextSize(8f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        drawTextWithinWidth(canvas, "Amount In Words:", totalsRect.left + 5f, wordsTop + 10f, textPaint,
                totalsRect.width() - 10f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        drawTextWithinWidth(canvas, amountWords(summary.getFinalTotal()), totalsRect.left + 5f, wordsTop + 22f,
                textPaint, totalsRect.width() - 10f);

        RectF notesRect = new RectF(content.left, top, totalsRect.left, top + Math.max(88f, totalsHeight));
        canvas.drawRect(notesRect, stroke);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(9f);
        textPaint.setColor(style.textPrimary);
        canvas.drawText("Terms And Conditions:", notesRect.left + 6f, notesRect.top + 14f, textPaint);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setTextSize(8.5f);
        textPaint.setColor(style.textMuted);

        List<String> wrapped = wrapText(
                safe(quote.getTermsAndConditions(), "Payment due on receipt."),
                textPaint,
                notesRect.width() - 12f);

        float textY = notesRect.top + 28f;
        for (int i = 0; i < wrapped.size() && i < 7; i++) {
            canvas.drawText(wrapped.get(i), notesRect.left + 6f, textY, textPaint);
            textY += 11f;
        }

        if (!TextUtils.isEmpty(profile.getFormatNotes())) {
            textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            canvas.drawText("Format: " + profile.getFormatNotes(), notesRect.left + 6f, notesRect.bottom - 8f,
                    textPaint);
        }

        float lowerTop = Math.max(notesRect.bottom, totalsRect.bottom) + 8f;
        float lowerHeight = 92f;

        float bankWidth = 160f;
        RectF bankRect = new RectF(content.left, lowerTop, content.left + bankWidth, lowerTop + lowerHeight);
        canvas.drawRect(bankRect, stroke);

        int alphaBankHeaderFill = (style.tableHeaderBackground & 0x00FFFFFF) | 0xCC000000;
        fill.setColor(alphaBankHeaderFill);
        canvas.drawRect(bankRect.left, bankRect.top, bankRect.right, bankRect.top + 18f, fill);
        textPaint.setColor(style.textPrimary);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(9f);
        canvas.drawText("BANK DETAILS", bankRect.left + 6f, bankRect.top + 13f, textPaint);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        textPaint.setTextSize(8f);
        drawTextWithinWidth(canvas, "BANK: " + safe(profile.getBankName(), "-"), bankRect.left + 6f, bankRect.top + 30f,
                textPaint, bankRect.width() - 10f);
        drawTextWithinWidth(canvas, "A/C: " + safe(profile.getBankAccountNumber(), "-"), bankRect.left + 6f,
                bankRect.top + 42f, textPaint, bankRect.width() - 10f);
        drawTextWithinWidth(canvas, "IFSC: " + safe(profile.getBankIfsc(), "-"), bankRect.left + 6f, bankRect.top + 54f,
                textPaint, bankRect.width() - 10f);
        drawTextWithinWidth(canvas, "BRANCH: " + safe(profile.getBankBranch(), "-"), bankRect.left + 6f,
                bankRect.top + 66f, textPaint, bankRect.width() - 10f);
        drawTextWithinWidth(canvas, "GST NO: " + safe(profile.getCompanyGstNumber(), "-"), bankRect.left + 6f,
                bankRect.top + 78f, textPaint, bankRect.width() - 10f);

        float signWidth = 138f;
        RectF signRect = new RectF(content.right - signWidth, lowerTop, content.right, lowerTop + lowerHeight);
        canvas.drawRect(signRect, stroke);
        fill.setColor(style.tableHeaderBackground);
        canvas.drawRect(signRect.left, signRect.top, signRect.right, signRect.top + 18f, fill);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(8.6f);
        drawTextWithinWidth(canvas, "For " + safe(profile.getCompanyName(), "Company"), signRect.left + 6f,
                signRect.top + 13f, textPaint, signRect.width() - 10f);

        Paint signLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        signLine.setColor(style.tableBorder);
        signLine.setStrokeWidth(1f);

        // Draw signature image if available
        if (signatureBitmap != null && !signatureBitmap.isRecycled()) {
            float targetH = 34f;
            float targetW = signRect.width() - 30f;
            float bW = signatureBitmap.getWidth();
            float bH = signatureBitmap.getHeight();
            float scale = Math.min(targetW / bW, targetH / bH);
            float rW = bW * scale;
            float rH = bH * scale;

            RectF sigDest = new RectF(
                    signRect.centerX() - (rW / 2f),
                    signRect.top + 50f - rH, // Just above the line
                    signRect.centerX() + (rW / 2f),
                    signRect.top + 50f);
            Paint sigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            sigPaint.setFilterBitmap(true);
            canvas.drawBitmap(signatureBitmap, null, sigDest, sigPaint);
        }

        canvas.drawLine(signRect.left + 20f, signRect.top + 52f, signRect.right - 20f, signRect.top + 52f, signLine);

        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setTextSize(9f);
        drawCenteredText(canvas, safe(profile.getSignatureName(), "Authorized Signatory"), signRect.left,
                signRect.right, signRect.top + 57f, 24f, textPaint);
    }

    private static void drawContinuationNote(Canvas canvas, RectF content, float top, QuoteTemplateStyle style) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textMuted);
        textPaint.setTextSize(10f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
        canvas.drawText("Continued on next page", content.right - 120f, top + 12f, textPaint);
    }

    private static void drawPageFooter(Canvas canvas, int pageNumber, int totalPages, RectF content,
            QuoteTemplateStyle style) {
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(style.tableBorder);
        canvas.drawLine(content.left, content.bottom - 22f, content.right, content.bottom - 22f, linePaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(style.textMuted);
        textPaint.setTextSize(9f);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        canvas.drawText("Generated by QuoteCraft", content.left, content.bottom - 8f, textPaint);

        String pageText = "Page " + pageNumber + " of " + totalPages;
        float width = textPaint.measureText(pageText);
        canvas.drawText(pageText, content.right - width, content.bottom - 8f, textPaint);
    }

    private static void drawWatermark(
            Canvas canvas,
            Quote quote,
            CompanyProfile profile,
            Bitmap watermarkLogoBitmap,
            float pageWidth,
            float pageHeight,
            QuoteTemplateStyle style) {
        if (!quote.isWatermarkEnabled()) {
            return;
        }

        int alphaPercent = profile.getWatermarkOpacityPercent();
        if (alphaPercent <= 0) {
            alphaPercent = quote.getWatermarkOpacityPercent();
        }
        // Default safe alpha if not set
        if (alphaPercent <= 0)
            alphaPercent = 15;
        alphaPercent = Math.max(5, Math.min(alphaPercent, 80));

        boolean logoMode = CompanyProfile.WATERMARK_MODE_LOGO.equalsIgnoreCase(profile.getWatermarkMode());
        if (logoMode
                && watermarkLogoBitmap != null
                && !watermarkLogoBitmap.isRecycled()
                && watermarkLogoBitmap.getWidth() > 0
                && watermarkLogoBitmap.getHeight() > 0) {

            // Make logo watermark larger and transparent (centered)
            float maxWidth = pageWidth * 0.75f;
            float maxHeight = pageHeight * 0.60f;

            float width = watermarkLogoBitmap.getWidth();
            float height = watermarkLogoBitmap.getHeight();
            float scale = Math.min(maxWidth / width, maxHeight / height);

            float renderWidth = width * scale;
            float renderHeight = height * scale;

            RectF target = new RectF(
                    (pageWidth - renderWidth) / 2f,
                    (pageHeight - renderHeight) / 2f,
                    (pageWidth + renderWidth) / 2f,
                    (pageHeight + renderHeight) / 2f);

            Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            logoPaint.setAlpha((int) (alphaPercent * 2.55f)); // Convert 0-100 to 0-255
            logoPaint.setFilterBitmap(true);
            canvas.drawBitmap(watermarkLogoBitmap, null, target, logoPaint);
            return;
        }

        String watermark = quote.getWatermarkText();
        if (TextUtils.isEmpty(watermark)) {
            watermark = profile.getWatermarkText();
        }
        if (TextUtils.isEmpty(watermark)) {
            return;
        }

        // Professional Text Watermark: Large, Angled, Faint
        Paint watermarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        watermarkPaint.setColor(style.accent);
        watermarkPaint.setAlpha((int) (alphaPercent * 2.55f));

        // Dynamic text size based on length to keep it impactful
        float textSize = 110f;
        if (watermark.length() > 10)
            textSize = 80f;
        if (watermark.length() > 20)
            textSize = 60f;

        watermarkPaint.setTextSize(textSize);
        watermarkPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        canvas.save();
        // Standard professional watermark angle
        canvas.rotate(-45f, pageWidth / 2f, pageHeight / 2f);

        float width = watermarkPaint.measureText(watermark);
        // Center the text
        canvas.drawText(watermark, (pageWidth - width) / 2f, pageHeight / 2f, watermarkPaint);
        canvas.restore();
    }

    private static float[] buildColumns(float left, float right, boolean hsnVisible) {
        float[] fractions;
        if (hsnVisible) {
            // # (0.05), Item (0.43), HSN (0.08), QTY (0.10), Unit (0.10), Rate (0.12),
            // Total (0.12)
            fractions = new float[] { 0.05f, 0.43f, 0.08f, 0.10f, 0.10f, 0.12f, 0.12f };
        } else {
            // # (0.05), Item (0.51), QTY (0.10), Unit (0.10), Rate (0.12), Total (0.12)
            fractions = new float[] { 0.05f, 0.51f, 0.10f, 0.10f, 0.12f, 0.12f };
        }

        float[] columns = new float[fractions.length + 1];
        columns[0] = left;

        float width = right - left;
        float cursor = left;
        for (int i = 0; i < fractions.length; i++) {
            cursor += width * fractions[i];
            columns[i + 1] = cursor;
        }
        columns[columns.length - 1] = right;
        return columns;
    }

    private static void drawCenteredText(Canvas canvas, String text, float left, float right, float top, float height,
            Paint paint) {
        float textWidth = paint.measureText(text);
        float x = left + ((right - left - textWidth) / 2f);
        float y = top + ((height / 2f) - ((paint.descent() + paint.ascent()) / 2f));
        canvas.drawText(text, x, y, paint);
    }

    private static void drawLeftText(Canvas canvas, String text, float x, float top, float height, Paint paint) {
        float y = top + ((height / 2f) - ((paint.descent() + paint.ascent()) / 2f));
        canvas.drawText(text, x, y, paint);
    }

    private static void drawRightText(Canvas canvas, String text, float rightX, float top, float height, Paint paint) {
        float textWidth = paint.measureText(text);
        float y = top + ((height / 2f) - ((paint.descent() + paint.ascent()) / 2f));
        canvas.drawText(text, rightX - textWidth, y, paint);
    }

    private static String safe(String value, String fallback) {
        if (TextUtils.isEmpty(value)) {
            return fallback;
        }
        return value;
    }

    private static String ellipsize(Paint paint, String text, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        int end = text.length();
        while (end > 0 && (paint.measureText(text, 0, end) + ellipsisWidth) > maxWidth) {
            end--;
        }
        if (end <= 0) {
            return ellipsis;
        }
        return text.substring(0, end) + ellipsis;
    }

    private static String formatDate(long dateMillis) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return format.format(new Date(dateMillis));
    }

    private static String formatNumber(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private static void drawTextWithinWidth(Canvas canvas, String text, float x, float y, Paint paint, float width) {
        String content = text == null ? "" : text;
        canvas.drawText(ellipsize(paint, content, width), x, y, paint);
    }

    private static String amountWords(double amount) {
        long value = Math.round(amount);
        if (value == 0) {
            return "Zero only";
        }
        return convert(value) + " only";
    }

    private static String convert(long n) {
        String[] units = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
                "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
        String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };

        if (n < 20) {
            return units[(int) n];
        }
        if (n < 100) {
            return tens[(int) (n / 10)] + ((n % 10 != 0) ? " " + convert(n % 10) : "");
        }
        if (n < 1000) {
            return convert(n / 100) + " Hundred" + ((n % 100 != 0) ? " " + convert(n % 100) : "");
        }
        if (n < 100000) {
            return convert(n / 1000) + " Thousand" + ((n % 1000 != 0) ? " " + convert(n % 1000) : "");
        }
        if (n < 10000000) {
            return convert(n / 100000) + " Lakh" + ((n % 100000 != 0) ? " " + convert(n % 100000) : "");
        }
        return convert(n / 10000000) + " Crore" + ((n % 10000000 != 0) ? " " + convert(n % 10000000) : "");
    }

    private static List<String> wrapText(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(next) <= maxWidth) {
                line.setLength(0);
                line.append(next);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static final class Row {
        final String label;
        final String value;

        Row(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public static final class PageSlice {
        private final int itemStart;
        private final int itemEnd;
        private final boolean firstPage;
        private final boolean lastPage;

        public PageSlice(int itemStart, int itemEnd, boolean firstPage, boolean lastPage) {
            this.itemStart = itemStart;
            this.itemEnd = itemEnd;
            this.firstPage = firstPage;
            this.lastPage = lastPage;
        }

        public int getItemStart() {
            return itemStart;
        }

        public int getItemEnd() {
            return itemEnd;
        }

        public boolean isFirstPage() {
            return firstPage;
        }

        public boolean isLastPage() {
            return lastPage;
        }
    }
}
