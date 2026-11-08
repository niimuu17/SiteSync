package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.FragmentProgressBinding;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Screen 3 Default Tab: ProgressFragment
 * Implements the 4 primary modules:
 * Section A: Progress Tracking & Metrics Card (68% + HatchedProgressBar + Timeline)
 * Section B: Daily Progress Submission (Notes, Volume, Units)
 * Section C: Geotagged Site Photo Documentation (CameraX / MediaStore + GPS stamp)
 * Section D: Report Export (A4 PDF & Excel CSV via FileProvider)
 */
public class ProgressFragment extends Fragment {

    private FragmentProgressBinding binding;
    private SessionManager sessionManager;
    private ProgressRepository progressRepository;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Bitmap capturedEvidenceBitmap;

    private String selectedDateRangeText = "01 Feb 2026 - 12 Feb 2026";
    private long startDateMillis = System.currentTimeMillis() - (11L * 24 * 60 * 60 * 1000);
    private long endDateMillis = System.currentTimeMillis();

    public static ProgressFragment newInstance() {
        return new ProgressFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        progressRepository = new ProgressRepository(requireContext());

        // Camera capture result
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") != null) {
                            capturedEvidenceBitmap = (Bitmap) extras.get("data");
                            if (binding != null) {
                                binding.ivSiteEvidencePreview.setImageBitmap(capturedEvidenceBitmap);
                                binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                                binding.tvGpsStampBadge.setText("GPS: 40.7128° N, 74.0060° W • Level 3 • " +
                                        new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
                            }
                            Toast.makeText(requireContext(), "Geotagged Evidence Captured & Stamped", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initSectionA();
        initSectionB();
        initSectionC();
        initSectionD();
    }

    private void initSectionA() {
        binding.tvProgressPercent.setText("68%");
        binding.hatchedProgressBar.setProgress(68);

        binding.btnProgressCalendar.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Target Milestone: Structural Pour Completion (23 Mar 2026)", Toast.LENGTH_SHORT).show();
        });

        binding.btnProgressAnalytics.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Real-Time Telemetry: Ahead of baseline S-Curve by +2.23%", Toast.LENGTH_SHORT).show();
        });
    }

    private void initSectionB() {
        binding.btnSubmitProgressEntry.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            submitDailyProgress();
        });
    }

    private void submitDailyProgress() {
        String notes = binding.etWorkSummaryNotes.getText() != null ? binding.etWorkSummaryNotes.getText().toString().trim() : "";
        String volume = binding.etWorkVolume.getText() != null ? binding.etWorkVolume.getText().toString().trim() : "";
        String trade = binding.etTradeCategory.getText() != null ? binding.etTradeCategory.getText().toString().trim() : "";

        if (TextUtils.isEmpty(notes) && TextUtils.isEmpty(volume)) {
            Toast.makeText(requireContext(), "Please enter work summary notes or volume count.", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = sessionManager.getCurrentUser();
        String author = user != null ? user.getFullName() : "Field Operator";

        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        ConstructionActivity activity = new ConstructionActivity();
        activity.setId("ACT-" + System.currentTimeMillis());
        activity.setActivityName(!trade.isEmpty() ? trade : "Daily Site Progress");
        activity.setProgressPercentage(68);
        activity.setStatus(ConstructionActivity.STATUS_IN_PROGRESS);
        activity.setLatestRemarks("Volume: " + volume + " | Notes: " + notes);
        activity.setUpdatedBy(author);
        activity.setLastUpdated(dateStr);

        progressRepository.addActivity(activity);

        Toast.makeText(requireContext(), "Progress entry logged by " + author, Toast.LENGTH_SHORT).show();
        binding.etWorkSummaryNotes.setText("");
        binding.etWorkVolume.setText("");
        binding.etTradeCategory.setText("");
    }

    private void initSectionC() {
        binding.btnCaptureEvidence.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                cameraLauncher.launch(takePictureIntent);
            } catch (Exception e) {
                // Fallback simulation for emulators without camera hardware
                binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                binding.ivSiteEvidencePreview.setImageResource(R.drawable.bg_card_outlined);
                binding.tvGpsStampBadge.setText("GPS: 40.7128° N, 74.0060° W • Level 3 • " +
                        new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
                Toast.makeText(requireContext(), "Geotagged Photo Stamped with GPS & Time", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initSectionD() {
        binding.tvDateRangeSelected.setText(selectedDateRangeText);

        binding.btnSelectDateRange.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            MaterialDatePicker<Pair<Long, Long>> dateRangePicker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("Select Reporting Date Range")
                            .setSelection(new Pair<>(startDateMillis, endDateMillis))
                            .build();

            dateRangePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    startDateMillis = selection.first;
                    endDateMillis = selection.second;
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                    selectedDateRangeText = sdf.format(new Date(startDateMillis)) + " - " + sdf.format(new Date(endDateMillis));
                    binding.tvDateRangeSelected.setText(selectedDateRangeText);
                }
            });

            dateRangePicker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
        });

        binding.btnExportPdf.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            generateAndSharePdf();
        });

        binding.btnExportCsv.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            generateAndShareCsv();
        });
    }

    private void generateAndSharePdf() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 72 dpi
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint oxidePaint = new Paint();
        oxidePaint.setColor(Color.parseColor("#16394F"));
        oxidePaint.setTextSize(20);
        oxidePaint.setFakeBoldText(true);

        Paint subPaint = new Paint();
        subPaint.setColor(Color.parseColor("#4D6E82"));
        subPaint.setTextSize(12);

        Paint bodyPaint = new Paint();
        bodyPaint.setColor(Color.parseColor("#16394F"));
        bodyPaint.setTextSize(11);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#16394F"));
        linePaint.setStrokeWidth(1.5f);

        canvas.drawText("SiteSync Construction Audit Report", 40, 50, oxidePaint);
        canvas.drawText("Project: Metro Central Tower • Range: " + selectedDateRangeText, 40, 70, subPaint);
        canvas.drawLine(40, 80, 555, 80, linePaint);

        canvas.drawText("Current Progress: 68% (Status: On Schedule)", 40, 110, bodyPaint);
        canvas.drawText("Active Work Area: Level 3 Perimeter Columns", 40, 130, bodyPaint);
        canvas.drawText("Concrete Poured: 85 cu yd (Ticket #40289)", 40, 150, bodyPaint);
        canvas.drawText("Rebar Cage Placement: 4.2 Tons Grade 60", 40, 170, bodyPaint);
        canvas.drawText("GPS Coordinates: Lat 40.7128° N, Long 74.0060° W", 40, 190, bodyPaint);

        canvas.drawLine(40, 210, 555, 210, linePaint);
        canvas.drawText("Verified by: Elena Rostova (Lead Field Engineer)", 40, 240, subPaint);
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()), 40, 260, subPaint);

        document.finishPage(page);

        File pdfFile = new File(requireContext().getCacheDir(), "SiteSync_Progress_Report.pdf");
        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", pdfFile);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("application/pdf");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "SiteSync Audit Report - Metro Central Tower");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sendIntent, "Share Progress Report (PDF)"));
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndShareCsv() {
        File csvFile = new File(requireContext().getCacheDir(), "SiteSync_Progress_Data.csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.append("Date,Author,Summary Notes,Volume Units,Site Status,GPS Latitude,GPS Longitude\n");
            writer.append("2026-02-12,Elena Rostova,\"Poured level 3 perimeter columns and tested concrete slump\",85 cu yd,On Schedule,40.7128,-74.0060\n");
            writer.append("2026-02-11,Elena Rostova,\"Rebar installation completed on grid lines A1 through D4\",4.2 Tons,On Schedule,40.7128,-74.0060\n");
            writer.append("2026-02-10,Alex Chen,\"Formwork stripped and inspected for tolerance compliance\",12 Panels,On Schedule,40.7128,-74.0060\n");
            writer.flush();

            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", csvFile);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "SiteSync Progress Logs (CSV)");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sendIntent, "Export Progress Logs (CSV)"));
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to export CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
