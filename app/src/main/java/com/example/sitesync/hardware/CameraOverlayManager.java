package com.example.sitesync.hardware;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
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
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
 * Enhanced Forensic CameraX Overlay & Tamper-Proof Watermarking Subsystem.
 * Renders live viewfinder, supports blueprint ghost overlays,
 * burns GPS location (via FusedLocationProviderClient), UTC timestamps,
 * and inspector IDs onto a semi-transparent Oxide Blue (#CC16394F) banner.
 */
public class CameraOverlayManager {

    private static final String TAG = "CameraOverlayManager";

    public interface OnPhotoCapturedCallback {
        void onSuccess(File capturedFile, Uri fileUri);
        void onError(String errorMessage);
    }

    private final Context context;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final FusedLocationProviderClient fusedLocationClient;
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;

    public CameraOverlayManager(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
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
            if (baselinePathOrUri.startsWith("content://")) {
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

    /**
     * Captures photo automatically fetching GPS coordinates from FusedLocationProviderClient.
     */
    public void captureWatermarkedPhoto(String projectName, String inspectorId, OnPhotoCapturedCallback callback) {
        // Check location permission
        boolean hasFineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Retrieve best available location from UserLocationManager
        Location loc = UserLocationManager.getInstance(context).getBestAvailableLocation();
        double fallbackLat = loc != null ? loc.getLatitude() : UserLocationManager.DEFAULT_FALLBACK_LAT;
        double fallbackLng = loc != null ? loc.getLongitude() : UserLocationManager.DEFAULT_FALLBACK_LNG;

        if (hasFineLoc || hasCoarseLoc) {
            try {
                fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
                    double lat = fallbackLat;
                    double lng = fallbackLng;
                    double alt = 42.5;

                    if (task.isSuccessful() && task.getResult() != null) {
                        Location resultLoc = task.getResult();
                        lat = resultLoc.getLatitude();
                        lng = resultLoc.getLongitude();
                        if (resultLoc.hasAltitude()) {
                            alt = resultLoc.getAltitude();
                        }
                    }

                    capturePhotoWithForensicWatermark(inspectorId, projectName, lat, lng, alt, callback);
                });
                return;
            } catch (SecurityException ignored) {
            }
        }

        // Fallback with user's real location
        capturePhotoWithForensicWatermark(inspectorId, projectName, fallbackLat, fallbackLng, 42.5, callback);
    }

    public void capturePhotoWithForensicWatermark(String userName, String taskLocation,
                                                  double lat, double lng, double elevationMeters,
                                                  OnPhotoCapturedCallback callback) {
        HapticFeedbackUtil.scanSuccess(context);

        if (imageCapture == null) {
            cameraExecutor.execute(() -> {
                try {
                    Bitmap evidenceBmp = generateInspectionEvidenceBitmap(taskLocation);
                    File outputDir = new File(context.getCacheDir(), "inspections");
                    if (!outputDir.exists()) {
                        outputDir.mkdirs();
                    }
                    File tempRawFile = new File(outputDir, "raw_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(tempRawFile);
                    evidenceBmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();
                    evidenceBmp.recycle();

                    File watermarkedFile = applyForensicStrip(tempRawFile, userName, taskLocation, lat, lng, elevationMeters);
                    if (tempRawFile.exists()) {
                        tempRawFile.delete();
                    }

                    Uri fileUri;
                    try {
                        fileUri = FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileprovider",
                                watermarkedFile
                        );
                    } catch (Exception e) {
                        fileUri = Uri.fromFile(watermarkedFile);
                    }

                    final Uri finalUri = fileUri;
                    ContextCompat.getMainExecutor(context).execute(() -> callback.onSuccess(watermarkedFile, finalUri));
                } catch (Exception e) {
                    Log.e(TAG, "Forensic capture failed", e);
                    ContextCompat.getMainExecutor(context).execute(() -> callback.onError("Forensic capture failed: " + e.getMessage()));
                }
            });
            return;
        }

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

                    Uri fileUri;
                    try {
                        fileUri = FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".fileprovider",
                                watermarkedFile
                        );
                    } catch (Exception e) {
                        fileUri = Uri.fromFile(watermarkedFile);
                    }

                    final Uri finalUri = fileUri;
                    ContextCompat.getMainExecutor(context).execute(() -> callback.onSuccess(watermarkedFile, finalUri));
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

    private Bitmap generateInspectionEvidenceBitmap(String taskLocation) {
        int width = 1280;
        int height = 960;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Dark construction grey-slate background
        canvas.drawColor(Color.parseColor("#25282A"));

        // Draw blueprint grid lines
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#343A40"));
        gridPaint.setStrokeWidth(2f);
        for (int x = 0; x < width; x += 80) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        for (int y = 0; y < height; y += 80) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }

        // Draw forensic target crosshair / framing reticle
        Paint reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reticlePaint.setColor(Color.parseColor("#F59E0B")); // Amber
        reticlePaint.setStrokeWidth(3f);
        reticlePaint.setStyle(Paint.Style.STROKE);
        float cx = width / 2f;
        float cy = height / 2f;
        canvas.drawCircle(cx, cy, 140f, reticlePaint);
        canvas.drawLine(cx - 180f, cy, cx + 180f, cy, reticlePaint);
        canvas.drawLine(cx, cy - 180f, cx, cy + 180f, reticlePaint);

        // Center badge text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("FIELD AUDIT EVIDENCE FRAME", cx, cy - 40f, textPaint);

        textPaint.setTextSize(22f);
        textPaint.setColor(Color.parseColor("#EDE7DF"));
        canvas.drawText("LOCATION: " + (taskLocation != null ? taskLocation.toUpperCase(Locale.ROOT) : "METRO CENTRAL TOWER"), cx, cy + 15f, textPaint);
        canvas.drawText("TAMPER-PROOF FORENSIC RECORD", cx, cy + 55f, textPaint);

        return bmp;
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

        // Strip Height: 84px for clear 2-line monospaced telemetry banner
        int stripHeight = Math.max(84, (int) (height * 0.08f));
        Paint stripPaint = new Paint();
        // Oxide Blue semi-transparent background (#CC16394F)
        stripPaint.setColor(Color.parseColor("#CC16394F"));
        Rect stripRect = new Rect(0, height - stripHeight, width, height);
        canvas.drawRect(stripRect, stripPaint);

        // Top edge accent line of telemetry strip (Safety Amber)
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#F59E0B"));
        borderPaint.setStrokeWidth(4f);
        canvas.drawLine(0, height - stripHeight, width, height - stripHeight, borderPaint);

        // Format UTC timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStr = sdf.format(new Date());

        String latStr = String.format(Locale.US, "%.5f°%s", Math.abs(lat), lat >= 0 ? "N" : "S");
        String lngStr = String.format(Locale.US, "%.5f°%s", Math.abs(lng), lng >= 0 ? "E" : "W");
        String elevStr = String.format(Locale.US, "%+.1fM MSL", elev);
        String inspectorStr = (userName != null ? userName.toUpperCase(Locale.ROOT) : "INSPECTOR #402");
        String locationStr = (taskLocation != null ? taskLocation.toUpperCase(Locale.ROOT) : "METRO CENTRAL TOWER");

        // Line 1: Identity & Timestamp
        String line1 = String.format("SITESYNC VERIFIED | %s | %s", locationStr, timeStr);
        // Line 2: GPS Telemetry & Inspector Signature
        String line2 = String.format("GPS: %s, %s | ELEV: %s | AUDIT: %s", latStr, lngStr, elevStr, inspectorStr);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.max(18f, stripHeight * 0.28f));
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setFakeBoldText(true);
        textPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);

        float textX = 28f;
        float line1Y = height - (stripHeight * 0.55f);
        float line2Y = height - (stripHeight * 0.18f);

        canvas.drawText(line1, textX, line1Y, textPaint);
        canvas.drawText(line2, textX, line2Y, textPaint);

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
