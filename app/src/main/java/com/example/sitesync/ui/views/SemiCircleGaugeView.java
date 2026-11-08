package com.example.sitesync.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * Custom Semi-Circular Telemetry Arc Gauge inspired by modern construction telemetry cockpits.
 * Renders a 180° or 240° curved gauge track, active progress sweep with rounded caps,
 * and central typography showing KPI percentage and trend delta.
 */
public class SemiCircleGaugeView extends View {

    private int progress = 0; // 0 to 100
    private int trackColor;
    private int strokeColor;
    private float strokeWidthPx;
    private float startAngle = 150f;
    private float sweepAngle = 240f;
    private String trendValue = "";
    private String metricTitle = "";

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPercentPaint;
    private Paint textTrendPaint;
    private Paint textTitlePaint;

    private final RectF arcBounds = new RectF();

    public SemiCircleGaugeView(Context context) {
        super(context);
        init(context, null);
    }

    public SemiCircleGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SemiCircleGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        // Defaults
        trackColor = ContextCompat.getColor(context, R.color.gauge_track_ash);
        strokeColor = ContextCompat.getColor(context, R.color.oxide_blue_primary);
        strokeWidthPx = dpToPx(9);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SemiCircleGaugeView);
            progress = a.getInt(R.styleable.SemiCircleGaugeView_gaugeProgress, 0);
            trackColor = a.getColor(R.styleable.SemiCircleGaugeView_gaugeTrackColor, trackColor);
            strokeColor = a.getColor(R.styleable.SemiCircleGaugeView_gaugeStrokeColor, strokeColor);
            strokeWidthPx = a.getDimension(R.styleable.SemiCircleGaugeView_gaugeStrokeWidth, strokeWidthPx);
            startAngle = a.getFloat(R.styleable.SemiCircleGaugeView_gaugeStartAngle, 150f);
            sweepAngle = a.getFloat(R.styleable.SemiCircleGaugeView_gaugeSweepAngle, 240f);
            trendValue = a.getString(R.styleable.SemiCircleGaugeView_trendValue);
            metricTitle = a.getString(R.styleable.SemiCircleGaugeView_metricTitle);
            a.recycle();
        }

        if (trendValue == null) trendValue = "";
        if (metricTitle == null) metricTitle = "";

        // Track Paint
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(trackColor);

        // Progress Paint
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(strokeColor);

        // Text Paints
        textPercentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPercentPaint.setColor(ContextCompat.getColor(context, R.color.text_oxide_primary));
        textPercentPaint.setTextAlign(Paint.Align.CENTER);
        textPercentPaint.setFakeBoldText(true);

        textTrendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTrendPaint.setColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
        textTrendPaint.setTextAlign(Paint.Align.CENTER);

        textTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTitlePaint.setColor(ContextCompat.getColor(context, R.color.text_oxide_secondary));
        textTitlePaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) h = (int) (w * 0.9f);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidthPx * 1.2f;
        float diameter = Math.min(w - pad * 2, (h - pad * 2) * 1.35f);
        float left = (w - diameter) / 2f;
        float top = pad + dpToPx(4);
        arcBounds.set(left, top, left + diameter, top + diameter);

        textPercentPaint.setTextSize(diameter * 0.28f);
        textTrendPaint.setTextSize(diameter * 0.13f);
        textTitlePaint.setTextSize(diameter * 0.13f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw Inactive Background Track Arc
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint);

        // 2. Draw Active Progress Sweep
        if (progress > 0) {
            float activeSweep = (Math.min(progress, 100) / 100f) * sweepAngle;
            canvas.drawArc(arcBounds, startAngle, activeSweep, false, progressPaint);
        }

        // 3. Central Percentage Text
        float centerX = arcBounds.centerX();
        float centerY = arcBounds.centerY() + (textPercentPaint.getTextSize() * 0.35f);

        canvas.drawText(progress + "%", centerX, centerY, textPercentPaint);

        // 4. Trend Delta
        if (!trendValue.isEmpty()) {
            if (trendValue.contains("▲") || trendValue.contains("+")) {
                textTrendPaint.setColor(Color.parseColor("#10B981"));
            } else if (trendValue.contains("▼") || trendValue.contains("-")) {
                textTrendPaint.setColor(Color.parseColor("#EF4444"));
            } else {
                textTrendPaint.setColor(Color.parseColor("#94A3B8"));
            }
            float trendY = arcBounds.top + (arcBounds.height() * 0.26f);
            canvas.drawText(trendValue, centerX, trendY, textTrendPaint);
        }

        // 5. Title / Label below
        if (!metricTitle.isEmpty()) {
            float titleY = arcBounds.bottom - dpToPx(2);
            canvas.drawText(metricTitle, centerX, titleY, textTitlePaint);
        }
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setTrendValue(String trend) {
        this.trendValue = trend != null ? trend : "";
        invalidate();
    }

    public void setMetricTitle(String title) {
        this.metricTitle = title != null ? title : "";
        invalidate();
    }

    public void setStrokeColor(int color) {
        this.strokeColor = color;
        this.progressPaint.setColor(color);
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
