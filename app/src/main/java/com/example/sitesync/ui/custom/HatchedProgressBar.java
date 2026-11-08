package com.example.sitesync.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;

/**
 * HatchedProgressBar: Industrial Warning Hazard Stripe Progress Bar
 * Strictly matches Section 4.2 Directive:
 * - 20dp pill-shaped container with rounded caps
 * - Completed portion clipped via Path.addRoundRect and filled with diagonal hazard stripe BitmapShader (45°)
 * - Vertical delay marker pin at cutoff threshold labeled "Delay: 12d" in #F97316
 */
public class HatchedProgressBar extends View {

    private int progress = 68; // 0 to 100
    private int stripeColor;
    private int trackColor;
    private String delayText = "Delay: 12d";

    private Paint trackPaint;
    private Paint borderPaint;
    private Paint stripePaint;
    private Paint pinPaint;
    private Paint pinTextPaint;

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
        stripeColor = ContextCompat.getColor(context, R.color.oxide_blue_primary);
        trackColor = ContextCompat.getColor(context, R.color.warm_ash_surface);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HatchedProgressBar);
            progress = a.getInt(R.styleable.HatchedProgressBar_hatchedProgress, 68);
            if (a.hasValue(R.styleable.HatchedProgressBar_hazardStripeColor)) {
                stripeColor = a.getColor(R.styleable.HatchedProgressBar_hazardStripeColor, stripeColor);
            } else if (a.hasValue(R.styleable.HatchedProgressBar_hatchedColor)) {
                stripeColor = a.getColor(R.styleable.HatchedProgressBar_hatchedColor, stripeColor);
            }
            if (a.hasValue(R.styleable.HatchedProgressBar_hazardTrackColor)) {
                trackColor = a.getColor(R.styleable.HatchedProgressBar_hazardTrackColor, trackColor);
            } else if (a.hasValue(R.styleable.HatchedProgressBar_hatchedTrackColor)) {
                trackColor = a.getColor(R.styleable.HatchedProgressBar_hatchedTrackColor, trackColor);
            }
            a.recycle();
        }

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(ContextCompat.getColor(context, R.color.stroke_oxide_blue));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));

        int pinColor = ContextCompat.getColor(context, R.color.oxide_blue_primary);

        pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinPaint.setColor(pinColor);
        pinPaint.setStrokeWidth(dpToPx(2f));
        pinPaint.setStyle(Paint.Style.STROKE);

        pinTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinTextPaint.setColor(pinColor);
        pinTextPaint.setTextSize(spToPx(11));
        pinTextPaint.setFakeBoldText(true);
        pinTextPaint.setTextAlign(Paint.Align.CENTER);

        setupStripeShader();
    }

    private void setupStripeShader() {
        int stripeWidth = (int) dpToPx(8);
        int tileWidth = stripeWidth * 2;
        Bitmap patternBitmap = Bitmap.createBitmap(tileWidth, tileWidth, Bitmap.Config.ARGB_8888);
        Canvas patternCanvas = new Canvas(patternBitmap);

        Paint bgTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgTilePaint.setColor(ContextCompat.getColor(getContext(), R.color.warm_ash_surface_alt));
        patternCanvas.drawRect(0, 0, tileWidth, tileWidth, bgTilePaint);

        Paint tileStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tileStripePaint.setColor(stripeColor);
        tileStripePaint.setStyle(Paint.Style.STROKE);
        tileStripePaint.setStrokeWidth(stripeWidth);

        // Diagonal lines across 45 degrees
        patternCanvas.drawLine(-tileWidth, tileWidth, tileWidth, -tileWidth, tileStripePaint);
        patternCanvas.drawLine(0, tileWidth * 2, tileWidth * 2, 0, tileStripePaint);
        patternCanvas.drawLine(0, tileWidth, tileWidth, 0, tileStripePaint);

        BitmapShader shader = new BitmapShader(patternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix matrix = new Matrix();
        matrix.postRotate(45f, tileWidth / 2f, tileWidth / 2f);
        shader.setLocalMatrix(matrix);

        stripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stripePaint.setShader(shader);
        stripePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = (int) dpToPx(42); // 20dp container + room for delay pin above/below
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int height = (heightMode == MeasureSpec.EXACTLY) ? heightSize : desiredHeight;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float totalHeight = getHeight();
        float barHeight = dpToPx(20);
        float barTop = (totalHeight - barHeight) / 2f + dpToPx(6);
        float barBottom = barTop + barHeight;
        float radius = barHeight / 2f;

        bounds.set(0, barTop, width, barBottom);

        // 1. Draw Inactive Track
        canvas.drawRoundRect(bounds, radius, radius, trackPaint);

        // 2. Draw Hatched Progress Fill
        float fillWidth = (Math.max(0, Math.min(100, progress)) / 100f) * width;
        if (fillWidth > 0) {
            fillBounds.set(0, barTop, fillWidth, barBottom);

            clipPath.reset();
            // Clip to pill container shape
            clipPath.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipPath);

            canvas.drawRect(fillBounds, stripePaint);

            canvas.restore();
        }

        // 3. Draw Border Outline
        canvas.drawRoundRect(bounds, radius, radius, borderPaint);

        // 4. Draw Vertical Delay Marker Pin at Cutoff Threshold
        if (fillWidth > 0 && fillWidth < width && delayText != null && !delayText.isEmpty()) {
            float pinX = fillWidth;
            float pinTop = barTop - dpToPx(14);
            float pinBottom = barBottom + dpToPx(3);

            // Vertical line
            canvas.drawLine(pinX, pinTop + dpToPx(12), pinX, pinBottom, pinPaint);

            // Delay Text Badge above cutoff pin
            float textX = Math.max(dpToPx(28), Math.min(width - dpToPx(28), pinX));
            canvas.drawText(delayText, textX, pinTop + dpToPx(9), pinTextPaint);
        }
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    public void setDelayText(String delayText) {
        this.delayText = delayText;
        invalidate();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
