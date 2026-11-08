package com.example.sitesync.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * BarcodeProgressView: Architectural High-Density Telemetry Barcode Indicator
 */
public class BarcodeProgressView extends View {

    private float progress = 68f; // 0 to 100
    private int completedColor;
    private int remainingColor;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarcodeProgressView(Context context) {
        super(context);
        init(context, null);
    }

    public BarcodeProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BarcodeProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        completedColor = ContextCompat.getColor(context, R.color.accent_safety_yellow);
        remainingColor = ContextCompat.getColor(context, R.color.gauge_track_dark);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BarcodeProgressView);
            progress = a.getInt(R.styleable.BarcodeProgressView_barcodeProgress, (int) progress);
            a.recycle();
        }

        barPaint.setStrokeWidth(dpToPx(2f));
        barPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setColors(int completedColor, int remainingColor) {
        this.completedColor = completedColor;
        this.remainingColor = remainingColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float step = dpToPx(4.5f);
        int totalBars = (int) (width / step);
        int completedBars = (int) (totalBars * (progress / 100f));

        for (int i = 0; i < totalBars; i++) {
            float x = i * step + (barPaint.getStrokeWidth() / 2f);
            if (i <= completedBars) {
                barPaint.setColor(completedColor);
            } else {
                barPaint.setColor(remainingColor);
            }
            canvas.drawLine(x, 0, x, height, barPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
