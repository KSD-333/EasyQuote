package com.example.quotationcreator.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.model.Quote;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

public final class PdfGenerator {

    private PdfGenerator() {
    }

    public static Uri exportToDownloads(
            Context context,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile
    ) throws IOException {
        byte[] pdfBytes = buildPdfBytes(context, quote, summary, profile);
        String fileName = defaultFileName(quote);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QuoteCraft");

            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Unable to create download entry");
            }

            try (OutputStream outputStream = resolver.openOutputStream(uri, "w")) {
                if (outputStream == null) {
                    throw new IOException("Unable to open output stream");
                }
                outputStream.write(pdfBytes);
                outputStream.flush();
            }
            return uri;
        }

        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) {
            downloadsDir = context.getFilesDir();
        }
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            throw new IOException("Unable to access export directory");
        }

        File pdfFile = new File(downloadsDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(pdfFile)) {
            outputStream.write(pdfBytes);
            outputStream.flush();
        }
        return Uri.fromFile(pdfFile);
    }

    public static Uri exportToCache(
            Context context,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile
    ) throws IOException {
        byte[] pdfBytes = buildPdfBytes(context, quote, summary, profile);
        String fileName = defaultFileName(quote);

        File cacheDir = new File(context.getCacheDir(), "saved_quotes");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Unable to create cache directory");
        }

        File savedFile = new File(cacheDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(savedFile)) {
            outputStream.write(pdfBytes);
            outputStream.flush();
        }

        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                savedFile
        );
    }

    public static void writeToUri(
            Context context,
            Uri uri,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile
    ) throws IOException {
        byte[] pdfBytes = buildPdfBytes(context, quote, summary, profile);
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri, "w")) {
            if (outputStream == null) {
                throw new IOException("Unable to open output stream");
            }
            outputStream.write(pdfBytes);
            outputStream.flush();
        }
    }

    public static Uri createShareablePdf(
            Context context,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile
    ) throws IOException {
        byte[] pdfBytes = buildPdfBytes(context, quote, summary, profile);
        String fileName = defaultFileName(quote);

        File cacheDir = new File(context.getCacheDir(), "shared_quotes");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Unable to create cache directory");
        }

        File shareFile = new File(cacheDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(shareFile)) {
            outputStream.write(pdfBytes);
            outputStream.flush();
        }

        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                shareFile
        );
    }

    private static byte[] buildPdfBytes(
            Context context,
            Quote quote,
            QuoteCalculator.Summary summary,
            CompanyProfile profile
    ) throws IOException {
        PdfDocument document = new PdfDocument();
        Bitmap watermarkLogoBitmap = loadWatermarkBitmap(context, profile);
        Bitmap companyLogoBitmap = loadCompanyLogoBitmap(context, profile);
        Bitmap signatureBitmap = loadSignatureBitmap(context, profile);
        try {
            List<QuoteRenderEngine.PageSlice> slices = QuoteRenderEngine.paginate(quote);
            int pageWidth = Math.round(QuoteRenderEngine.pageWidth(quote));
            int pageHeight = Math.round(QuoteRenderEngine.pageHeight(quote));
            for (int i = 0; i < slices.size(); i++) {
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        i + 1
                ).create();

                PdfDocument.Page page = document.startPage(pageInfo);
                QuoteRenderEngine.drawPage(
                        page.getCanvas(),
                        quote,
                        summary,
                    profile,
                    watermarkLogoBitmap,
                    companyLogoBitmap,
                    signatureBitmap,
                        slices.get(i),
                        i + 1,
                        slices.size()
                );
                document.finishPage(page);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.writeTo(output);
            return output.toByteArray();
        } finally {
            if (watermarkLogoBitmap != null && !watermarkLogoBitmap.isRecycled()) {
                watermarkLogoBitmap.recycle();
            }
            if (companyLogoBitmap != null && !companyLogoBitmap.isRecycled()) {
                companyLogoBitmap.recycle();
            }
            if (signatureBitmap != null && !signatureBitmap.isRecycled()) {
                signatureBitmap.recycle();
            }
            document.close();
        }
    }

    private static Bitmap loadWatermarkBitmap(Context context, CompanyProfile profile) {
        if (context == null || profile == null) {
            return null;
        }
        if (!CompanyProfile.WATERMARK_MODE_LOGO.equalsIgnoreCase(profile.getWatermarkMode())) {
            return null;
        }
        if (TextUtils.isEmpty(profile.getWatermarkLogoUri())) {
            return null;
        }

        return ImageDecodeUtils.decodeSampledBitmap(context, profile.getWatermarkLogoUri(), 1600, 1600);
    }

    private static Bitmap loadCompanyLogoBitmap(Context context, CompanyProfile profile) {
        if (context == null || profile == null || TextUtils.isEmpty(profile.getCompanyLogoUri())) {
            return null;
        }

        return ImageDecodeUtils.decodeSampledBitmap(context, profile.getCompanyLogoUri(), 900, 900);
    }

    private static Bitmap loadSignatureBitmap(Context context, CompanyProfile profile) {
        if (context == null || profile == null || TextUtils.isEmpty(profile.getSignatureImageUri())) {
            return null;
        }

        return ImageDecodeUtils.decodeSampledBitmap(context, profile.getSignatureImageUri(), 600, 600);
    }

    public static String defaultFileName(Quote quote) {
        String quoteNumber = quote.getQuotationNumber();
        if (quoteNumber == null || quoteNumber.trim().isEmpty()) {
            quoteNumber = "quote";
        }

        String sanitized = quoteNumber.replaceAll("[^A-Za-z0-9_-]", "_");
        String suffix = String.format(Locale.US, "%d", System.currentTimeMillis());
        return sanitized + "_" + suffix + ".pdf";
    }
}
