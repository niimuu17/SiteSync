package com.example.sitesync.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom stadium-shaped progress bar featuring diagonal safety/hazard stripes,
 * a dynamic split between completed (amber) and remaining (grey) segments,
 * and an integrated vertical milestone/delay tick label.
 */
public class StripedPillProgressView extends View {

    private float progress = 68f; // Percentage: 0 to 100
    private String delayLabel = "Delay 126";

    private final Paint amberBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint amberStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint greyBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint greyStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path clipPath = new Path();
    private final RectF bounds = new RectF();

    public StripedPillProgressView(Context context) {
        super(context);
        init();
    }

    public StripedPillProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StripedPillProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        amberBgPaint.setColor(Color.parseColor("#F7A600"));
        amberStripePaint.setColor(Color.parseColor("#FFF3D6"));
        amberStripePaint.setStrokeWidth(dpToPx(6));

        greyBgPaint.setColor(Color.parseColor("#EDEDED"));
        greyStripePaint.setColor(Color.parseColor("#DEDEDE"));
        greyStripePaint.setStrokeWidth(dpToPx(6));

        dividerPaint.setColor(Color.parseColor("#141414"));
        dividerPaint.setStrokeWidth(dpToPx(2.5f));

        labelBgPaint.setColor(Color.parseColor("#141414"));

        labelTextPaint.setColor(Color.parseColor("#FFFFFF"));
        labelTextPaint.setTextSize(dpToPx(9.5f));
        labelTextPaint.setFakeBoldText(true);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setDelayLabel(String delayLabel) {
        this.delayLabel = delayLabel;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bounds.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(bounds, h / 2f, h / 2f, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float splitX = width * (progress / 100f);

        canvas.save();
        canvas.clipPath(clipPath);

        // 1. Draw Completed Segment (Amber + Diagonal Stripes)
        canvas.save();
        canvas.clipRect(0, 0, splitX, height);
        canvas.drawRect(0, 0, splitX, height, amberBgPaint);

        float stripeSpacing = dpToPx(12);
        for (float x = -height; x < splitX + height; x += stripeSpacing) {
            canvas.drawLine(x, height, x + height, 0, amberStripePaint);
        }
        canvas.restore();

        // 2. Draw Remaining Segment (Light Grey + Diagonal Stripes)
        canvas.save();
        canvas.clipRect(splitX, 0, width, height);
        canvas.drawRect(splitX, 0, width, height, greyBgPaint);

        for (float x = splitX - height; x < width + height; x += stripeSpacing) {
            canvas.drawLine(x, height, x + height, 0, greyStripePaint);
        }
        canvas.restore();

        // 3. Draw Vertical Divider at Split
        canvas.drawLine(splitX, 0, splitX, height, dividerPaint);

        canvas.restore(); // Exit capsule clip
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
