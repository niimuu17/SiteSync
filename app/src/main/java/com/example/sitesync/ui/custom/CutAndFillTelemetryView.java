package com.example.sitesync.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * CutAndFillTelemetryView: Candlestick/T-cap Elevation Bar Chart
 * Strictly matches Section 4.2 Directive:
 * - Vertical elevation bars with horizontal T-caps
 * - Coordinate labels below each bar: -15 Ft, -10 Ft, -5.12 Ft, 00 Ft, 14 Ft
 * - Alternates bar colors: Terracotta (#E0532B) for cut, Safety Yellow (#F59E0B) for fill
 * - Displays summary metric at the base: "Net: 224,400 cu yd"
 */
public class CutAndFillTelemetryView extends View {

    public static class StemPoint {
        public final String label;
        public final float topRatio;    // 0.0 to 1.0
        public final float bottomRatio; // 0.0 to 1.0
        public final int color;
        public final boolean isCut; // true = Cut (Terracotta), false = Fill (Safety Yellow)

        public StemPoint(String label, float topRatio, float bottomRatio, int color, boolean isCut) {
            this.label = label;
            this.topRatio = topRatio;
            this.bottomRatio = bottomRatio;
            this.color = color;
            this.isCut = isCut;
        }
    }

    private StemPoint[] stems;
    private String netMetric = "Net: 224,400 cu yd";
    private int cutColor = Color.parseColor("#E0532B");
    private int fillColor = Color.parseColor("#F59E0B");

    private final Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint summaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CutAndFillTelemetryView(Context context) {
        super(context);
        init(context, null);
    }

    public CutAndFillTelemetryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CutAndFillTelemetryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CutAndFillTelemetryView);
            if (a.hasValue(R.styleable.CutAndFillTelemetryView_netVolumeText)) {
                netMetric = a.getString(R.styleable.CutAndFillTelemetryView_netVolumeText);
            }
            cutColor = a.getColor(R.styleable.CutAndFillTelemetryView_cutColor, cutColor);
            fillColor = a.getColor(R.styleable.CutAndFillTelemetryView_fillColor, fillColor);
            a.recycle();
        }

        stemPaint.setStrokeWidth(dpToPx(4f));
        capPaint.setStrokeWidth(dpToPx(3f));

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(spToPx(12));
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        summaryPaint.setColor(ContextCompat.getColor(context, R.color.accent_safety_yellow));
        summaryPaint.setTextSize(spToPx(13));
        summaryPaint.setFakeBoldText(true);
        summaryPaint.setTextAlign(Paint.Align.CENTER);

        gridLinePaint.setColor(ContextCompat.getColor(context, R.color.stroke_card_border));
        gridLinePaint.setStrokeWidth(dpToPx(1f));

        // Coordinate labels specified in directive: -15 Ft, -10 Ft, -5.12 Ft, 00 Ft, 14 Ft
        stems = new StemPoint[]{
                new StemPoint("-15 Ft", 0.32f, 0.65f, cutColor, true),
                new StemPoint("-10 Ft", 0.22f, 0.58f, fillColor, false),
                new StemPoint("-5.12 Ft", 0.38f, 0.72f, cutColor, true),
                new StemPoint("00 Ft", 0.28f, 0.62f, fillColor, false),
                new StemPoint("14 Ft", 0.18f, 0.50f, fillColor, false)
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = (int) dpToPx(160);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : desiredHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (stems == null || stems.length == 0) return;

        int width = getWidth();
        int height = getHeight();

        float sidePadding = dpToPx(32);
        float chartBottom = height - dpToPx(44); // Space for labels and bottom net metric
        float chartHeight = chartBottom - dpToPx(16);
        float capRadius = dpToPx(9);

        // Subtle zero/ground reference line
        float groundY = chartBottom * 0.55f;
        canvas.drawLine(sidePadding * 0.5f, groundY, width - sidePadding * 0.5f, groundY, gridLinePaint);

        float availableWidth = width - (sidePadding * 2);
        float stepX = availableWidth / (stems.length - 1);

        for (int i = 0; i < stems.length; i++) {
            StemPoint pt = stems[i];
            float x = sidePadding + (i * stepX);

            float topY = dpToPx(16) + (chartHeight * pt.topRatio);
            float bottomY = dpToPx(16) + (chartHeight * pt.bottomRatio);

            int color = pt.isCut ? cutColor : fillColor;

            // Draw Vertical Stem Bar
            stemPaint.setColor(color);
            canvas.drawLine(x, topY, x, bottomY, stemPaint);

            // Draw Top Horizontal T-Cap
            capPaint.setColor(color);
            canvas.drawLine(x - capRadius, topY, x + capRadius, topY, capPaint);

            // Draw Bottom Horizontal T-Cap
            canvas.drawLine(x - capRadius, bottomY, x + capRadius, bottomY, capPaint);

            // Coordinate label below each bar
            canvas.drawText(pt.label, x, chartBottom + dpToPx(14), labelPaint);
        }

        // Summary metric at the base: "Net: 224,400 cu yd"
        if (netMetric != null && !netMetric.isEmpty()) {
            canvas.drawText(netMetric, width / 2f, height - dpToPx(8), summaryPaint);
        }
    }

    public void setNetMetric(String netMetric) {
        this.netMetric = netMetric;
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
