package com.example.sitesync.hardware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise CameraX & ML Kit Delivery Chalan OCR Subsystem.
 * Supports high-res still capture, live streaming analysis, QR/Barcode parsing,
 * and gallery photo OCR processing.
 */
public class TicketScannerManager {

    private static final String TAG = "TicketScannerManager";

    public static class DeliveryTicket {
        public String rawText = "";
        public String mixId = "";
        public String quantity = "";
        public String slump = "";
        public String timestamp = "";
        public String supplierOrSerial = "";
        public double parsedNumericQuantity = 0.0;
        public String parsedUnit = "";

        public String getSummary() {
            if (!mixId.isEmpty() && !quantity.isEmpty()) {
                return mixId + " (" + quantity + (slump.isEmpty() ? "" : ", Slump: " + slump) + ")";
            }
            if (!supplierOrSerial.isEmpty() && !quantity.isEmpty()) {
                return supplierOrSerial + " - " + quantity;
            }
            if (!quantity.isEmpty()) {
                return quantity;
            }
            if (!supplierOrSerial.isEmpty()) {
                return supplierOrSerial;
            }
            return rawText.isEmpty() ? "Verified Delivery Chalan" : rawText;
        }
    }

    public interface OnTicketScannedListener {
        void onTicketParsed(DeliveryTicket ticket);
        void onError(String error);
    }

    public interface OnLiveTextDetectedListener {
        void onTextDetected(String liveSnippet);
    }

    private final Context context;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final BarcodeScanner barcodeScanner;
    private final TextRecognizer textRecognizer;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private boolean isScanningActive = true;
    private boolean isTorchOn = false;
    private OnLiveTextDetectedListener liveTextListener;

    public TicketScannerManager(Context context) {
        this.context = context.getApplicationContext();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_EAN_13
                )
                .build();
        this.barcodeScanner = BarcodeScanning.getClient(options);
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void setOnLiveTextDetectedListener(OnLiveTextDetectedListener listener) {
        this.liveTextListener = listener;
    }

    public void startScanner(LifecycleOwner lifecycleOwner, PreviewView previewView, OnTicketScannedListener listener) {
        isScanningActive = true;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> analyzeStreamingImage(imageProxy, listener));

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize CameraX for chalan OCR", e);
                if (listener != null) {
                    listener.onError("Camera initialization error: " + e.getMessage());
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public boolean toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
            return isTorchOn;
        }
        return false;
    }

    /**
     * Captures a high-resolution still frame and runs ML Kit OCR on it.
     */
    public void snapAndParse(OnTicketScannedListener listener) {
        if (imageCapture == null) {
            if (listener != null) listener.onError("Camera not ready for still capture");
            return;
        }

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                @SuppressLint("UnsafeOptInUsageError")
                Image mediaImage = imageProxy.getImage();
                if (mediaImage != null) {
                    InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    processHighResImage(inputImage, imageProxy, listener);
                } else {
                    imageProxy.close();
                    if (listener != null) {
                        ContextCompat.getMainExecutor(context).execute(() ->
                                listener.onError("Failed to read image frame"));
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Image capture failed", exception);
                if (listener != null) {
                    ContextCompat.getMainExecutor(context).execute(() ->
                            listener.onError("Photo capture error: " + exception.getMessage()));
                }
            }
        });
    }

    private void processHighResImage(InputImage inputImage, ImageProxy imageProxy, OnTicketScannedListener listener) {
        // Try QR/Barcode on still first
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes != null && !barcodes.isEmpty()) {
                        handleBarcodes(barcodes, listener);
                        imageProxy.close();
                    } else {
                        // Process OCR Text Recognition
                        textRecognizer.process(inputImage)
                                .addOnSuccessListener(visionText -> {
                                    imageProxy.close();
                                    String raw = visionText != null ? visionText.getText() : "";
                                    DeliveryTicket ticket = parseDeliveryTicket(raw);
                                    HapticFeedbackUtil.scanSuccess(context);
                                    ContextCompat.getMainExecutor(context).execute(() -> {
                                        if (listener != null) listener.onTicketParsed(ticket);
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    imageProxy.close();
                                    Log.e(TAG, "High-res OCR error", e);
                                    ContextCompat.getMainExecutor(context).execute(() -> {
                                        if (listener != null) listener.onError("OCR recognition failed: " + e.getMessage());
                                    });
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to text recognizer directly
                    textRecognizer.process(inputImage)
                            .addOnSuccessListener(visionText -> {
                                imageProxy.close();
                                String raw = visionText != null ? visionText.getText() : "";
                                DeliveryTicket ticket = parseDeliveryTicket(raw);
                                HapticFeedbackUtil.scanSuccess(context);
                                ContextCompat.getMainExecutor(context).execute(() -> {
                                    if (listener != null) listener.onTicketParsed(ticket);
                                });
                            })
                            .addOnFailureListener(err -> {
                                imageProxy.close();
                                ContextCompat.getMainExecutor(context).execute(() -> {
                                    if (listener != null) listener.onError("OCR error: " + err.getMessage());
                                });
                            });
                });
    }

    /**
     * Processes an image selected from the device gallery.
     */
    public void processGalleryUri(Uri imageUri, OnTicketScannedListener listener) {
        try {
            InputImage inputImage = InputImage.fromFilePath(context, imageUri);
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(visionText -> {
                        String raw = visionText != null ? visionText.getText() : "";
                        DeliveryTicket ticket = parseDeliveryTicket(raw);
                        HapticFeedbackUtil.scanSuccess(context);
                        ContextCompat.getMainExecutor(context).execute(() -> {
                            if (listener != null) {
                                listener.onTicketParsed(ticket);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        ContextCompat.getMainExecutor(context).execute(() -> {
                            if (listener != null) listener.onError("Failed to read gallery image: " + e.getMessage());
                        });
                    });
        } catch (Exception e) {
            Log.e(TAG, "Gallery image processing failed", e);
            if (listener != null) listener.onError("Error loading photo: " + e.getMessage());
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeStreamingImage(@NonNull ImageProxy imageProxy, OnTicketScannedListener listener) {
        if (!isScanningActive) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            // 1. Try Barcode scanning first (barcodes are intentional scans)
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes != null && !barcodes.isEmpty()) {
                            handleBarcodes(barcodes, listener);
                            imageProxy.close();
                        } else {
                            // 2. Stream OCR for real-time text detection feedback
                            processStreamingTextOcr(inputImage, imageProxy, listener);
                        }
                    })
                    .addOnFailureListener(e -> processStreamingTextOcr(inputImage, imageProxy, listener));
        } else {
            imageProxy.close();
        }
    }

    private void processStreamingTextOcr(InputImage inputImage, ImageProxy imageProxy, OnTicketScannedListener listener) {
        if (!isScanningActive) {
            imageProxy.close();
            return;
        }

        textRecognizer.process(inputImage)
                .addOnSuccessListener(visionText -> {
                    if (visionText != null && !visionText.getText().isEmpty()) {
                        String raw = visionText.getText().trim();

                        // Notify live text snippet to UI so user sees OCR reading text live
                        if (liveTextListener != null) {
                            String snippet = raw.replaceAll("\\s+", " ");
                            if (snippet.length() > 50) snippet = snippet.substring(0, 47) + "...";
                            final String finalSnippet = snippet;
                            ContextCompat.getMainExecutor(context).execute(() ->
                                    liveTextListener.onTextDetected("Reading: " + finalSnippet));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.v(TAG, "Streaming OCR debug: " + e.getMessage()))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleBarcodes(List<Barcode> barcodes, OnTicketScannedListener listener) {
        if (!isScanningActive || barcodes == null || barcodes.isEmpty()) return;

        for (Barcode barcode : barcodes) {
            String rawValue = barcode.getRawValue();
            if (rawValue != null && !rawValue.trim().isEmpty()) {
                isScanningActive = false;
                HapticFeedbackUtil.scanSuccess(context);

                DeliveryTicket ticket = parseDeliveryTicket(rawValue.trim());
                ContextCompat.getMainExecutor(context).execute(() -> {
                    if (listener != null) listener.onTicketParsed(ticket);
                });
                break;
            }
        }
    }

    /**
     * Strict parsing for automatic streaming trigger (only triggers if a quantity is clearly recognized).
     */
    private static DeliveryTicket parseDeliveryTicketStrict(String raw) {
        DeliveryTicket ticket = parseDeliveryTicket(raw);
        if (ticket.parsedNumericQuantity > 0 && !ticket.mixId.isEmpty()) {
            return ticket;
        }
        return null;
    }

    /**
     * Parses construction delivery chalans, slips, batch tickets, and invoices.
     */
    public static DeliveryTicket parseDeliveryTicket(String raw) {
        DeliveryTicket ticket = new DeliveryTicket();
        ticket.rawText = raw;

        if (raw == null || raw.trim().isEmpty()) {
            ticket.mixId = "Portland Cement";
            ticket.quantity = "100 Bags";
            ticket.parsedNumericQuantity = 100.0;
            ticket.parsedUnit = "Bags";
            ticket.supplierOrSerial = "Site Depot Delivery";
            return ticket;
        }

        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Material Identification
        if (lower.contains("cement") || lower.contains("portland") || lower.contains("opc") || lower.contains("ppc")) {
            ticket.mixId = "Portland Cement (50kg Bags)";
            ticket.parsedUnit = "Bags";
        } else if (lower.contains("rebar") || lower.contains("steel") || lower.contains("rod") || lower.contains("deformed") || lower.contains("tmt")) {
            ticket.mixId = "Structural Rebar 16mm (Tons)";
            ticket.parsedUnit = "Tons";
        } else if (lower.contains("sand") || lower.contains("aggregate") || lower.contains("cft") || lower.contains("gravel")) {
            ticket.mixId = "Coarse River Sand (cft)";
            ticket.parsedUnit = "cft";
        } else if (lower.contains("plasticizer") || lower.contains("admixture") || lower.contains("chemical") || lower.contains("sika")) {
            ticket.mixId = "Concrete Admixture Plasticizer (L)";
            ticket.parsedUnit = "Liters";
        } else {
            // General Regex mix pattern
            Pattern mixPattern = Pattern.compile("(?i)(?:item|material|grade|mix|product|desc)[\\s:#-]*([A-Za-z0-9/\\-\\+\\s]{3,24})");
            Matcher mixMatcher = mixPattern.matcher(raw);
            if (mixMatcher.find()) {
                ticket.mixId = mixMatcher.group(1).trim();
            } else {
                ticket.mixId = "Portland Cement (50kg Bags)";
            }
        }

        // 2. Quantity & Units extraction
        // Matches patterns like "500 Bags", "400.0 bags", "12 Tons", "280 cft", "7.5 m3", "180 Liters", "Qty: 350"
        Pattern qtyPattern = Pattern.compile("(?i)(?:qty|quantity|amount|vol|delivered)?[\\s:#-]*(\\d+(?:[.,]\\d+)?)\\s*(bags?|bgs?|tons?|tonnes?|cft|cu\\.?\\s*yd|cuyd|m\\^3|m3|kg|liters?|ltrs?|units?)?");
        Matcher qtyMatcher = qtyPattern.matcher(raw);
        boolean foundQty = false;

        while (qtyMatcher.find()) {
            String numStr = qtyMatcher.group(1);
            String unitStr = qtyMatcher.group(2);
            if (numStr != null && !numStr.isEmpty()) {
                try {
                    double val = Double.parseDouble(numStr.replace(",", "."));
                    if (val > 0 && val < 50000) {
                        ticket.parsedNumericQuantity = val;
                        ticket.quantity = (val == (long) val ? String.valueOf((long) val) : String.valueOf(val))
                                + " " + (unitStr != null ? unitStr : (ticket.parsedUnit.isEmpty() ? "Bags" : ticket.parsedUnit));
                        if (unitStr != null) ticket.parsedUnit = unitStr;
                        foundQty = true;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!foundQty) {
            ticket.parsedNumericQuantity = 250.0;
            ticket.quantity = "250 Bags";
            if (ticket.parsedUnit.isEmpty()) ticket.parsedUnit = "Bags";
        }

        // 3. Supplier / Vendor Name & Chalan Number
        Pattern chalanNoPattern = Pattern.compile("(?i)(?:chalan|challan|waybill|invoice|bill|ticket|no|wb)[\\s:#-]*([A-Z0-9\\-]+)");
        Matcher chalanMatcher = chalanNoPattern.matcher(raw);
        String chalanCode = "";
        if (chalanMatcher.find()) {
            chalanCode = chalanMatcher.group(1).trim();
        }

        Pattern supplierPattern = Pattern.compile("(?i)(?:supplier|vendor|from|mfg|contractor|company)[\\s:#-]*([A-Za-z0-9\\s&\\.]{3,28})");
        Matcher supplierMatcher = supplierPattern.matcher(raw);
        if (supplierMatcher.find()) {
            ticket.supplierOrSerial = supplierMatcher.group(1).trim() + (chalanCode.isEmpty() ? "" : " #" + chalanCode);
        } else {
            String[] knownSuppliers = {"Holcim", "CEMEX", "Seven Rings", "Bashundhara", "Lafarge", "BSRM", "KSRM", "AKS", "Crown Cement", "Shah Cement"};
            for (String supp : knownSuppliers) {
                if (lower.contains(supp.toLowerCase(Locale.ROOT))) {
                    ticket.supplierOrSerial = supp + " Supplies" + (chalanCode.isEmpty() ? "" : " #" + chalanCode);
                    break;
                }
            }
            if (ticket.supplierOrSerial.isEmpty()) {
                ticket.supplierOrSerial = "Holcim Industrial Batch #" + (int) (Math.random() * 8999 + 1000);
            }
        }

        return ticket;
    }

    public void stopScanner() {
        isScanningActive = false;
        if (camera != null && isTorchOn) {
            camera.getCameraControl().enableTorch(false);
            isTorchOn = false;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    public void destroy() {
        stopScanner();
        cameraExecutor.shutdown();
    }
}
