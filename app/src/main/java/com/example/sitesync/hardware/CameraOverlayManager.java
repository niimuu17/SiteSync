package com.example.sitesync.hardware;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Feature 3: "Match Angle" Progress Comparison Camera (CameraX)
 * Renders live viewfinder with semi-transparent baseline ghost overlay.
 * Burns forensic telemetry strip on bottom 60px of captured photos (quality = 88%).
 */
public class CameraOverlayManager {

    private static final String TAG = "CameraOverlayManager";

    public interface OnPhotoCapturedCallback {
        void onSuccess(File capturedFile, Uri fileUri);
        void onError(String errorMessage);
    }

    private final Context context;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    public CameraOverlayManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startCamera(LifecycleOwner lifecycleOwner, PreviewView previewView, Runnable onReady) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(previewView.getDisplay() != null ? previewView.getDisplay().getRotation() : 0)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture);

                if (onReady != null) {
                    onReady.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "CameraX initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void setGhostBaselineImage(ImageView overlayImageView, String baselinePathOrUri) {
        if (overlayImageView == null) return;
        if (baselinePathOrUri == null || baselinePathOrUri.isEmpty()) {
            overlayImageView.setImageDrawable(null);
            return;
        }

        try {
            if (baselinePathOrUri.startsWith("http")) {
                // Remote image URL
                // Let image loader or fallback handle
            } else if (baselinePathOrUri.startsWith("content://")) {
                overlayImageView.setImageURI(Uri.parse(baselinePathOrUri));
            } else {
                File file = new File(baselinePathOrUri);
                if (file.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                    overlayImageView.setImageBitmap(bmp);
                }
            }
            overlayImageView.setAlpha(0.40f);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load ghost baseline image: " + e.getMessage());
        }
    }

    public void capturePhotoWithForensicWatermark(String userName, String taskLocation,
                                                  double lat, double lng, double elevationMeters,
                                                  OnPhotoCapturedCallback callback) {
        if (imageCapture == null) {
            callback.onError("Camera is not initialized yet.");
            return;
        }

        HapticFeedbackUtil.scanSuccess(context);

        File outputDir = new File(context.getCacheDir(), "inspections");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File tempRawFile = new File(outputDir, "raw_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(tempRawFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                try {
                    File watermarkedFile = applyForensicStrip(tempRawFile, userName, taskLocation, lat, lng, elevationMeters);
                    if (tempRawFile.exists()) {
                        tempRawFile.delete();
                    }
                    Uri uri = Uri.fromFile(watermarkedFile);
                    ContextCompat.getMainExecutor(context).execute(() -> callback.onSuccess(watermarkedFile, uri));
                } catch (Exception e) {
                    Log.e(TAG, "Forensic stamping failed", e);
                    ContextCompat.getMainExecutor(context).execute(() -> callback.onError("Forensic stamping failed: " + e.getMessage()));
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                ContextCompat.getMainExecutor(context).execute(() -> callback.onError("Capture failed: " + exception.getMessage()));
            }
        });
    }

    private File applyForensicStrip(File rawFile, String userName, String taskLocation,
                                    double lat, double lng, double elev) throws IOException {
        Bitmap original = BitmapFactory.decodeFile(rawFile.getAbsolutePath());
        if (original == null) {
            throw new IOException("Cannot decode raw image file");
        }

        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap mutableBmp = original.copy(Bitmap.Config.ARGB_8888, true);
        original.recycle();

        Canvas canvas = new Canvas(mutableBmp);

        // Strip Height: 60px
        int stripHeight = 60;
        Paint stripPaint = new Paint();
        stripPaint.setColor(Color.parseColor("#E6111317")); // 90% dark slate
        Rect stripRect = new Rect(0, height - stripHeight, width, height);
        canvas.drawRect(stripRect, stripPaint);

        // Top edge accent line of telemetry strip (Safety Yellow)
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#F59E0B"));
        borderPaint.setStrokeWidth(3f);
        canvas.drawLine(0, height - stripHeight, width, height - stripHeight, borderPaint);

        // Watermark Text:
        // SITESYNC VERIFIED | 2026-02-12 14:32:01 UTC | LAT: 40.7128N LNG: 74.0060W | ELEV: +42.4M | USER: ELENA ROSTOVA (ENG-04)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStr = sdf.format(new Date());

        String latStr = String.format(Locale.US, "%.4f%s", Math.abs(lat), lat >= 0 ? "N" : "S");
        String lngStr = String.format(Locale.US, "%.4f%s", Math.abs(lng), lng >= 0 ? "E" : "W");
        String elevStr = String.format(Locale.US, "%+.1fM", elev);
        String userStr = (userName != null ? userName.toUpperCase() : "FIELD ENGINEER");

        String telemetryLine = String.format("SITESYNC VERIFIED | %s | LAT: %s LNG: %s | ELEV: %s | USER: %s",
                timeStr, latStr, lngStr, elevStr, userStr);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK);

        canvas.drawText(telemetryLine, 24, height - 22, textPaint);

        // Save re-compressed (quality = 88%)
        File finalFile = new File(context.getCacheDir(), "inspections/IMG_VERIFIED_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = new FileOutputStream(finalFile);
        mutableBmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
        fos.flush();
        fos.close();
        mutableBmp.recycle();

        return finalFile;
    }

    public void shutdown() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        cameraExecutor.shutdown();
    }
}
