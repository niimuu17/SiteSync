package com.example.sitesync.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom Candlestick/Stem Elevation Delta Chart for Earthwork Cut & Fill Telemetry.
 * Renders vertical elevation stems with horizontal T-caps in alternating Terracotta and Amber.
 */
public class CutAndFillChartView extends View {

    public static class StemData {
        public final String label;
        public final float topRatio;    // 0 to 1
        public final float bottomRatio; // 0 to 1
        public final int color;
        public final boolean labelAtTop;

        public StemData(String label, float topRatio, float bottomRatio, int color, boolean labelAtTop) {
            this.label = label;
            this.topRatio = topRatio;
            this.bottomRatio = bottomRatio;
            this.color = color;
            this.labelAtTop = labelAtTop;
        }
    }

    private StemData[] stems;

    private final Paint stemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint capPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CutAndFillChartView(Context context) {
        super(context);
        init();
    }

    public CutAndFillChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CutAndFillChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        stemPaint.setStrokeWidth(dpToPx(3.5f));
        capPaint.setStrokeWidth(dpToPx(2.5f));

        labelPaint.setColor(Color.parseColor("#141414"));
        labelPaint.setTextSize(dpToPx(11f));
        labelPaint.setFakeBoldText(true);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        baselinePaint.setColor(Color.parseColor("#F0F0EE"));
        baselinePaint.setStrokeWidth(dpToPx(1f));

        // Default construction elevation points from reference visual
        stems = new StemData[]{
                new StemData("-15 Ft", 0.35f, 0.70f, Color.parseColor("#E0532B"), false),
                new StemData("-10 Ft", 0.38f, 0.75f, Color.parseColor("#F7A600"), true),
                new StemData("-5.12 Ft", 0.40f, 0.78f, Color.parseColor("#F28B66"), false),
                new StemData("-3.10 Ft", 0.38f, 0.72f, Color.parseColor("#F7A600"), true),
                new StemData("14 Ft", 0.42f, 0.74f, Color.parseColor("#F7A600"), true),
                new StemData("00 Ft", 0.45f, 0.72f, Color.parseColor("#E0532B"), false)
        };
    }

    public void setStems(StemData[] stems) {
        this.stems = stems;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (stems == null || stems.length == 0) return;

        int width = getWidth();
        int height = getHeight();
        float paddingSide = dpToPx(24);
        float capRadius = dpToPx(7);

        float availableWidth = width - (paddingSide * 2);
        float stepX = availableWidth / (stems.length - 1);

        for (int i = 0; i < stems.length; i++) {
            StemData item = stems[i];
            float x = paddingSide + (i * stepX);

            float topY = height * item.topRatio;
            float bottomY = height * item.bottomRatio;

            // Draw Stem
            stemPaint.setColor(item.color);
            canvas.drawLine(x, topY, x, bottomY, stemPaint);

            // Draw Top Cap
            capPaint.setColor(item.color);
            canvas.drawLine(x - capRadius, topY, x + capRadius, topY, capPaint);

            // Draw Bottom Cap
            canvas.drawLine(x - capRadius, bottomY, x + capRadius, bottomY, capPaint);

            // Draw Label
            if (item.labelAtTop) {
                canvas.drawText(item.label, x, topY - dpToPx(8), labelPaint);
            } else {
                canvas.drawText(item.label, x, bottomY + dpToPx(16), labelPaint);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
