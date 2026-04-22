package com.example.quotationcreator.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.quotationcreator.R;
import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.model.Quote;
import com.example.quotationcreator.util.ImageDecodeUtils;
import com.example.quotationcreator.util.QuoteCalculator;
import com.example.quotationcreator.util.QuoteRenderEngine;

import java.util.List;

public class QuotePreviewView extends View {

    private Quote quote;
    private QuoteCalculator.Summary summary;
    private CompanyProfile companyProfile;
    private Bitmap watermarkBitmap;
    private Bitmap companyLogoBitmap;
    private Bitmap signatureBitmap;

    private ScaleGestureDetector scaleDetector;
    private float zoomScale = 1f;
    private float panX = 0f;
    private float panY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isDragging;
    private Boolean lastLandscapeMode;

    private static final float MIN_ZOOM = 1f;
    private static final float MAX_ZOOM = 4f;

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public QuotePreviewView(Context context) {
        super(context);
        init();
    }

    public QuotePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuotePreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        framePaint.setStyle(Paint.Style.FILL);
        framePaint.setColor(Color.WHITE);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(0x16000000);

        placeholderPaint.setColor(getResources().getColor(R.color.text_secondary, getContext().getTheme()));
        placeholderPaint.setTextSize(32f);
        placeholderPaint.setTextAlign(Paint.Align.CENTER);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                float factor = detector.getScaleFactor();

                float lastScale = zoomScale;
                zoomScale *= factor;
                zoomScale = Math.max(MIN_ZOOM, Math.min(zoomScale, MAX_ZOOM));

                // Effective factor after clamping
                float actualFactor = zoomScale / lastScale;

                // Adjust pan to keep focal point fixed
                panX += (focusX - (getWidth() / 2f + panX)) * (1 - actualFactor);
                panY += (focusY - (getHeight() / 2f + panY)) * (1 - actualFactor);

                invalidate();
                return true;
            }
        });
    }

    public void setQuoteData(@Nullable Quote quote, @Nullable QuoteCalculator.Summary summary, @Nullable CompanyProfile companyProfile) {
        boolean orientationChanged = false;
        if (quote != null) {
            boolean nextLandscape = quote.isLandscapeMode();
            orientationChanged = lastLandscapeMode == null || lastLandscapeMode != nextLandscape;
            lastLandscapeMode = nextLandscape;
        }

        this.quote = quote == null ? null : quote.copy();
        this.summary = summary;
        this.companyProfile = companyProfile == null ? null : companyProfile.copy();
        refreshCompanyBitmaps();
        if (orientationChanged) {
            resetZoomAndPan();
        }
        invalidate();
    }

    public void resetZoomAndPan() {
        zoomScale = 1f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (quote == null) {
            return super.onTouchEvent(event);
        }

        boolean handled = scaleDetector.onTouchEvent(event);
        if (scaleDetector.isInProgress()) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    panX += dx;
                    panY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(getResources().getColor(R.color.surface_background, getContext().getTheme()));

        if (quote == null) {
            canvas.drawText("Live preview appears here", getWidth() / 2f, getHeight() / 2f, placeholderPaint);
            return;
        }

        float pageWidth = QuoteRenderEngine.pageWidth(quote);
        float pageHeight = QuoteRenderEngine.pageHeight(quote);

        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight() - 16f;
        float availableHeight = getHeight() - getPaddingTop() - getPaddingBottom() - 16f;

        float fitScale = Math.min(availableWidth / pageWidth, availableHeight / pageHeight);
        float totalScale = fitScale * zoomScale;

        clampPan(totalScale, pageWidth, pageHeight, availableWidth, availableHeight);

        canvas.save();
        float centerX = getWidth() / 2f + panX;
        float centerY = getHeight() / 2f + panY;
        canvas.translate(centerX, centerY);
        canvas.scale(totalScale, totalScale);
        canvas.translate(-pageWidth / 2f, -pageHeight / 2f);

        // Subtle shadow
        canvas.drawRect(6f, 6f, pageWidth + 6f, pageHeight + 6f, shadowPaint);
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, framePaint);

        List<QuoteRenderEngine.PageSlice> slices = QuoteRenderEngine.paginate(quote);
        if (!slices.isEmpty()) {
            QuoteRenderEngine.drawPage(canvas, quote, summary, companyProfile, watermarkBitmap, companyLogoBitmap, signatureBitmap, slices.get(0), 1, slices.size());
        }
        canvas.restore();

        if (slices.size() > 1) {
            placeholderPaint.setTextSize(26f);
            canvas.drawText("Preview shows page 1", getWidth() / 2f, getHeight() - 16f, placeholderPaint);
            placeholderPaint.setTextSize(32f);
        }
    }

    private void clampPan(float totalScale, float pageWidth, float pageHeight, float availableWidth, float availableHeight) {
        float contentWidth = pageWidth * totalScale;
        float contentHeight = pageHeight * totalScale;

        // X-axis: Center if fits, otherwise clamp edges
        if (contentWidth <= availableWidth) {
            panX = 0f;
        } else {
            float maxPanX = (contentWidth - availableWidth) / 2f;
            panX = Math.max(-maxPanX, Math.min(maxPanX, panX));
        }

        // Y-axis: Align to TOP if it fits or by default preference, otherwise clamp edges
        if (contentHeight <= availableHeight) {
            // Calculate panY needed to align top of content with top of view
            panY = (contentHeight - availableHeight) / 2f;
        } else {
            float maxPanY = (contentHeight - availableHeight) / 2f;
            panY = Math.max(-maxPanY, Math.min(maxPanY, panY));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleCompanyBitmaps();
    }

    private void refreshCompanyBitmaps() {
        recycleCompanyBitmaps();
        if (companyProfile == null) {
            return;
        }

        if (!TextUtils.isEmpty(companyProfile.getCompanyLogoUri())) {
            companyLogoBitmap = ImageDecodeUtils.decodeSampledBitmap(getContext(), companyProfile.getCompanyLogoUri(), 900, 900);
        }

        refreshSignatureBitmap();

        if (!CompanyProfile.WATERMARK_MODE_LOGO.equalsIgnoreCase(companyProfile.getWatermarkMode())) {
            return;
        }
        if (TextUtils.isEmpty(companyProfile.getWatermarkLogoUri())) {
            return;
        }

        watermarkBitmap = ImageDecodeUtils.decodeSampledBitmap(getContext(), companyProfile.getWatermarkLogoUri(), 1600, 1600);
    }

    private void refreshSignatureBitmap() {
        if (signatureBitmap != null && !signatureBitmap.isRecycled()) {
            signatureBitmap.recycle();
        }
        signatureBitmap = null;
        if (companyProfile != null && !TextUtils.isEmpty(companyProfile.getSignatureImageUri())) {
            signatureBitmap = ImageDecodeUtils.decodeSampledBitmap(getContext(), companyProfile.getSignatureImageUri(), 600, 600);
        }
    }

    private void recycleCompanyBitmaps() {
        if (companyLogoBitmap != null && !companyLogoBitmap.isRecycled()) {
            companyLogoBitmap.recycle();
        }
        companyLogoBitmap = null;

        if (watermarkBitmap != null && !watermarkBitmap.isRecycled()) {
            watermarkBitmap.recycle();
        }
        watermarkBitmap = null;

        if (signatureBitmap != null && !signatureBitmap.isRecycled()) {
            signatureBitmap.recycle();
        }
        signatureBitmap = null;
    }
}
