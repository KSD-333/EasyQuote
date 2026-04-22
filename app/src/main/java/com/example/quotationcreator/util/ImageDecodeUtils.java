package com.example.quotationcreator.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

public final class ImageDecodeUtils {

    private ImageDecodeUtils() {
    }

    @Nullable
    public static Bitmap decodeSampledBitmap(@NonNull Context context, @Nullable String uriValue, int reqWidth, int reqHeight) {
        if (TextUtils.isEmpty(uriValue)) {
            return null;
        }

        try {
            Uri uri = Uri.parse(uriValue);

            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
                if (boundsStream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
            }

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return null;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, reqWidth, reqHeight);
            decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            decodeOptions.inDither = true;

            try (InputStream decodeStream = context.getContentResolver().openInputStream(uri)) {
                if (decodeStream == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int safeReqWidth = Math.max(1, reqWidth);
        int safeReqHeight = Math.max(1, reqHeight);

        int inSampleSize = 1;
        while ((height / inSampleSize) > safeReqHeight || (width / inSampleSize) > safeReqWidth) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }
    /**
     * Copies a file from a URI to the app's internal storage and makes the white background transparent.
     * Perfect for signatures and logos on paper.
     */
    @Nullable
    public static String saveSignatureTransparently(@NonNull Context context, @Nullable Uri uri, @NonNull String fileName) {
        if (uri == null) return null;
        try {
            // Load the bitmap
            Bitmap source = decodeSampledBitmap(context, uri.toString(), 2000, 2000);
            if (source == null) return null;

            // Process to remove white-ish background
            Bitmap transparent = makeBackgroundTransparent(source);
            source.recycle();

            java.io.File dir = new java.io.File(context.getFilesDir(), "logos");
            if (!dir.exists() && !dir.mkdirs()) return null;

            java.io.File destFile = new java.io.File(dir, fileName);
            try (java.io.FileOutputStream os = new java.io.FileOutputStream(destFile)) {
                transparent.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
            }
            transparent.recycle();
            return Uri.fromFile(destFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap makeBackgroundTransparent(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        // Simple threshold based transparency (treats very bright pixels as transparent)
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            // If it's nearly white, make it transparent
            if (r > 230 && g > 230 && b > 230) {
                pixels[i] = 0x00FFFFFF; // Fully transparent
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    /**
     * Copies a file from a URI to the app's internal storage to ensure persistent access.
     */
    @Nullable
    public static String saveImageLocally(@NonNull Context context, @Nullable Uri uri, @NonNull String fileName) {
        if (uri == null) return null;
        try {
            java.io.File dir = new java.io.File(context.getFilesDir(), "logos");
            if (!dir.exists() && !dir.mkdirs()) return null;

            java.io.File destFile = new java.io.File(dir, fileName);
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream os = new java.io.FileOutputStream(destFile)) {
                if (is == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return Uri.fromFile(destFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
