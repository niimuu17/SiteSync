package com.example.sitesync.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * SemiCircleGaugeView: Industrial Telemetry Arc Gauge
 * Strictly matches Section 4.2 Directive:
 * - Extends View
 * - Oval: RectF(strokeWidth, strokeWidth, width - strokeWidth, (height * 2) - strokeWidth)
 * - Track arc: 180 to 180
 * - Active sweep arc: 180 to sweepAngle
 * - Centered big-number text (40sp bold #FFFFFF)
 * - Trend indicator below it (+2.23 ▲ in #10B981 or -1.00 ▼ in #EF4444)
 */
public class SemiCircleGaugeView extends View {

    private int progress = 85;
    private int max = 100;
    private int trackColor;
    private int sweepColor;
    private float strokeWidth;
    private String trendValue = "+2.23 ▲";
    private String metricTitle = "";

    private Paint trackPaint;
    private Paint sweepPaint;
    private Paint textNumberPaint;
    private Paint textTrendPaint;
    private Paint textTitlePaint;

    private final RectF oval = new RectF();

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
        trackColor = ContextCompat.getColor(context, R.color.gauge_track_ash);
        sweepColor = ContextCompat.getColor(context, R.color.oxide_blue_primary);
        strokeWidth = dpToPx(9);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SemiCircleGaugeView);
            progress = a.getInt(R.styleable.SemiCircleGaugeView_gaugeProgress, 85);
            max = a.getInt(R.styleable.SemiCircleGaugeView_gaugeMax, 100);
            trackColor = a.getColor(R.styleable.SemiCircleGaugeView_gaugeTrackColor, trackColor);
            if (a.hasValue(R.styleable.SemiCircleGaugeView_gaugeSweepColor)) {
                sweepColor = a.getColor(R.styleable.SemiCircleGaugeView_gaugeSweepColor, sweepColor);
            } else if (a.hasValue(R.styleable.SemiCircleGaugeView_gaugeStrokeColor)) {
                sweepColor = a.getColor(R.styleable.SemiCircleGaugeView_gaugeStrokeColor, sweepColor);
            }
            strokeWidth = a.getDimension(R.styleable.SemiCircleGaugeView_gaugeStrokeWidth, strokeWidth);
            if (a.hasValue(R.styleable.SemiCircleGaugeView_trendValue)) {
                trendValue = a.getString(R.styleable.SemiCircleGaugeView_trendValue);
            }
            if (a.hasValue(R.styleable.SemiCircleGaugeView_metricTitle)) {
                metricTitle = a.getString(R.styleable.SemiCircleGaugeView_metricTitle);
            }
            a.recycle();
        }

        if (trendValue == null) trendValue = "";
        if (metricTitle == null) metricTitle = "";

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(trackColor);

        sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sweepPaint.setStyle(Paint.Style.STROKE);
        sweepPaint.setStrokeWidth(strokeWidth);
        sweepPaint.setStrokeCap(Paint.Cap.ROUND);
        sweepPaint.setColor(sweepColor);

        int oxidePrimary = ContextCompat.getColor(context, R.color.text_oxide_primary);
        int oxideSecondary = ContextCompat.getColor(context, R.color.text_oxide_secondary);

        textNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textNumberPaint.setColor(oxidePrimary);
        textNumberPaint.setTextAlign(Paint.Align.CENTER);
        textNumberPaint.setFakeBoldText(true);
        textNumberPaint.setTextSize(spToPx(34));

        textTrendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTrendPaint.setTextAlign(Paint.Align.CENTER);
        textTrendPaint.setFakeBoldText(true);
        textTrendPaint.setTextSize(spToPx(13));
        textTrendPaint.setColor(oxideSecondary);

        textTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTitlePaint.setColor(oxideSecondary);
        textTitlePaint.setTextAlign(Paint.Align.CENTER);
        textTitlePaint.setTextSize(spToPx(12));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int mode = MeasureSpec.getMode(heightMeasureSpec);

        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED || height == 0) {
            height = (int) (width * 0.58f);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        // 1. Arc bounds calculation
        float halfStroke = strokeWidth / 2f + dpToPx(4);
        oval.set(halfStroke, halfStroke, width - halfStroke, (height * 2) - halfStroke);

        // 2. Background Track
        canvas.drawArc(oval, 180, 180, false, trackPaint);

        // 3. Active Sweep
        float ratio = max > 0 ? (float) progress / max : 0;
        ratio = Math.max(0f, Math.min(1f, ratio));
        float sweepAngle = 180f * ratio;
        if (sweepAngle > 0) {
            canvas.drawArc(oval, 180, sweepAngle, false, sweepPaint);
        }

        // 4. Centered Big-Number Text
        float centerX = width / 2f;
        float centerY = height * 0.70f;
        canvas.drawText(progress + "%", centerX, centerY, textNumberPaint);

        // 5. Trend Indicator below / around number
        if (!trendValue.isEmpty()) {
            textTrendPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_oxide_secondary));
            float trendY = height * 0.90f;
            canvas.drawText(trendValue, centerX, trendY, textTrendPaint);
        }

        // 6. Optional Title
        if (!metricTitle.isEmpty()) {
            float titleY = height * 0.35f;
            canvas.drawText(metricTitle, centerX, titleY, textTitlePaint);
        }
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(max, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setMax(int max) {
        this.max = max > 0 ? max : 100;
        invalidate();
    }

    public void setSweepColor(int color) {
        this.sweepColor = color;
        this.sweepPaint.setColor(color);
        invalidate();
    }

    public void setTrendValue(String trend) {
        this.trendValue = trend != null ? trend : "";
        invalidate();
    }

    public void setMetricTitle(String title) {
        this.metricTitle = title != null ? title : "";
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
