package com.example.sitesync.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom corner-anchored radial telemetry arc gauge.
 * Draws an arc along the bottom-right corner quadrant with a leading node indicator dot.
 */
public class CornerArcGaugeView extends View {

    private float progress = 85f; // 0 to 100
    private int trackColor = Color.parseColor("#3340454B");
    private int progressColor = Color.parseColor("#F7A600");

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF arcRect = new RectF();

    public CornerArcGaugeView(Context context) {
        super(context);
        init();
    }

    public CornerArcGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CornerArcGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float strokeWidth = dpToPx(3.5f);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(trackColor);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#FFF3D6"));
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public void setProgressColor(int color) {
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float stroke = dpToPx(4f);
        // Anchor arc bounding box around the bottom-right corner
        float radius = Math.min(width, height) - stroke;
        arcRect.set(width - radius * 1.6f, height - radius * 1.6f, width + radius * 0.4f, height + radius * 0.4f);

        // Total sweep angle = 90 degrees (from 180° to 270°)
        float startAngle = 180f;
        float totalSweep = 90f;
        float currentSweep = totalSweep * (progress / 100f);

        // Draw background faint track
        canvas.drawArc(arcRect, startAngle, totalSweep, false, trackPaint);

        // Draw active telemetry progress arc
        canvas.drawArc(arcRect, startAngle, currentSweep, false, progressPaint);

        // Draw leading indicator node dot at tip
        double angleRad = Math.toRadians(startAngle + currentSweep);
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        float dotRadius = arcRect.width() / 2f;

        float dotX = (float) (centerX + dotRadius * Math.cos(angleRad));
        float dotY = (float) (centerY + dotRadius * Math.sin(angleRad));

        canvas.drawCircle(dotX, dotY, dpToPx(4.5f), dotPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
