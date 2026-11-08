package com.example.sitesync.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * Custom Hatched Progress Bar featuring diagonal 45° industrial safety stripes,
 * rounded pill contours, and customizable hazard stripe colors.
 */
public class HatchedProgressBar extends View {

    private int progress = 68; // 0 to 100
    private int stripeColor;
    private int trackColor;
    private float stripeWidthPx;
    private float stripeSpacingPx;

    private Paint trackPaint;
    private Paint stripePaint;
    private Paint borderPaint;
    private Paint progressBackgroundPaint;

    private final Path clipPath = new Path();
    private final RectF bounds = new RectF();
    private final RectF fillBounds = new RectF();

    public HatchedProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public HatchedProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HatchedProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        stripeColor = ContextCompat.getColor(context, R.color.accent_safety_yellow);
        trackColor = ContextCompat.getColor(context, R.color.gauge_track_dark);
        stripeWidthPx = dpToPx(7);
        stripeSpacingPx = dpToPx(8);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HatchedProgressBar);
            progress = a.getInt(R.styleable.HatchedProgressBar_hatchedProgress, 68);
            stripeColor = a.getColor(R.styleable.HatchedProgressBar_hatchedColor, stripeColor);
            trackColor = a.getColor(R.styleable.HatchedProgressBar_hatchedTrackColor, trackColor);
            stripeWidthPx = a.getDimension(R.styleable.HatchedProgressBar_stripeWidth, stripeWidthPx);
            stripeSpacingPx = a.getDimension(R.styleable.HatchedProgressBar_stripeSpacing, stripeSpacingPx);
            a.recycle();
        }

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        progressBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressBackgroundPaint.setColor(ContextCompat.getColor(context, R.color.accent_hazard_orange));
        progressBackgroundPaint.setStyle(Paint.Style.FILL);

        stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripePaint.setColor(stripeColor);
        stripePaint.setStyle(Paint.Style.STROKE);
        stripePaint.setStrokeWidth(stripeWidthPx);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        borderPaint.setColor(ContextCompat.getColor(context, R.color.stroke_card_border));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (h == 0) h = (int) dpToPx(38);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        bounds.set(0, 0, w, h);

        // 1. Clip outer rounded pill shape
        clipPath.reset();
        clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);

        // 2. Draw inactive track
        canvas.drawRect(bounds, trackPaint);

        // 3. Draw active filled region with hazard stripes
        float fillWidth = (Math.max(0, Math.min(100, progress)) / 100f) * w;
        if (fillWidth > 0) {
            fillBounds.set(0, 0, fillWidth, h);

            canvas.save();
            canvas.clipRect(fillBounds);

            // Subtle base tint for the filled region
            canvas.drawRect(fillBounds, progressBackgroundPaint);

            // Diagonal 45° stripes
            float totalStep = stripeWidthPx + stripeSpacingPx;
            float startX = -h;
            float endX = fillWidth + h;

            for (float x = startX; x <= endX; x += totalStep) {
                canvas.drawLine(x, h, x + h, 0, stripePaint);
            }

            canvas.restore();
        }

        canvas.restore();

        // 4. Draw outer border outline
        canvas.drawRoundRect(bounds, radius, radius, borderPaint);
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setStripeColor(int color) {
        this.stripeColor = color;
        this.stripePaint.setColor(color);
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
