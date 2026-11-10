package com.example.sitesync.ui.cockpit;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.ui.MainActivity;
import com.example.sitesync.hardware.VoiceDictationManager;
import com.example.sitesync.models.DailySiteReport;
import com.example.sitesync.models.TechnicalIssue;
import com.example.sitesync.ui.dialogs.RfiChatBottomSheet;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.PdfReportGenerator;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Tab 4: 5-Section Daily Site Report & Live Technical Issue / RFI Stream
 * Integrates "Speak Note" voice dictation, "Scan Delivery Ticket" barcode scanner,
 * and vector A4 PDF export/sharing for Project Managers.
 */
public class DailyLogTabFragment extends Fragment {

    private EditText etDailyWeather;
    private EditText etDailyCompletedWork;
    private ImageButton btnVoiceDictateWork;
    private EditText etDailyManpower;
    private EditText etDailyMaterials;
    private MaterialButton btnScanTicketMaterials;
    private EditText etDailyEquipment;
    private MaterialButton btnSaveDraftReport;
    private MaterialButton btnSubmitReportPdf;

    private MaterialButton btnOpenRfiChat1;
    private MaterialButton btnOpenRfiChat2;

    private VoiceDictationManager voiceManager;
    private boolean isListening = false;
    private DailyReportRepository reportRepository;

    public static DailyLogTabFragment newInstance() {
        return new DailyLogTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_log_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reportRepository = new DailyReportRepository(requireContext());

        etDailyWeather = view.findViewById(R.id.etDailyWeather);
        etDailyCompletedWork = view.findViewById(R.id.etDailyCompletedWork);
        btnVoiceDictateWork = view.findViewById(R.id.btnVoiceDictateWork);
        etDailyManpower = view.findViewById(R.id.etDailyManpower);
        etDailyMaterials = view.findViewById(R.id.etDailyMaterials);
        btnScanTicketMaterials = view.findViewById(R.id.btnScanTicketMaterials);
        etDailyEquipment = view.findViewById(R.id.etDailyEquipment);
        btnSaveDraftReport = view.findViewById(R.id.btnSaveDraftReport);
        btnSubmitReportPdf = view.findViewById(R.id.btnSubmitReportPdf);

        btnOpenRfiChat1 = view.findViewById(R.id.btnOpenRfiChat1);
        btnOpenRfiChat2 = view.findViewById(R.id.btnOpenRfiChat2);

        setupVoiceDictation();

        // Scan Delivery Ticket Integration
        btnScanTicketMaterials.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            String[] options = {
                "Simulate Verified Challan (Shah Cement & BSRM)",
                "Open Full Inventory Dashboard"
            };
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Challan & Delivery Ticket")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            String scannedNote = "[DELIVERY CHALAN: Shah Cement Special - 120 Bags, BSRM 16mm - 4.5 Ton, Supplier: Meghna Mills]";
                            String existing = etDailyMaterials.getText() != null ? etDailyMaterials.getText().toString().trim() : "";
                            etDailyMaterials.setText(existing.isEmpty() ? scannedNote : existing + "\n" + scannedNote);
                            Toast.makeText(requireContext(), "Ticket Appended to Materials", Toast.LENGTH_SHORT).show();
                        } else {
                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).selectNavigationTab(R.id.nav_resource);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Save Draft Report
        btnSaveDraftReport.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            DailySiteReport report = buildReportFromInputs(DailySiteReport.STATUS_DRAFT);
            reportRepository.saveReport(report);
            Toast.makeText(requireContext(), "Daily Report Draft Saved", Toast.LENGTH_SHORT).show();
        });

        // Submit & Share PDF Report
        btnSubmitReportPdf.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(requireContext(), v);
            DailySiteReport report = buildReportFromInputs(DailySiteReport.STATUS_SUBMITTED);
            reportRepository.saveReport(report);
            PdfReportGenerator.shareReportPdf(requireContext(), report);
        });

        // RFI Live Chat Thread 1
        btnOpenRfiChat1.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            TechnicalIssue issue1 = new TechnicalIssue();
            issue1.setId("RFI-2026-042");
            issue1.setTitle("HVAC Duct Clash with Beam B-4");
            issue1.setStatus("OPEN");
            issue1.setPriority("HIGH");
            issue1.setSiteLocation("Building 2 • Level 14 Core");
            RfiChatBottomSheet sheet = RfiChatBottomSheet.newInstance(issue1);
            sheet.show(getParentFragmentManager(), "RfiChatBottomSheet");
        });

        // RFI Live Chat Thread 2
        btnOpenRfiChat2.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            TechnicalIssue issue2 = new TechnicalIssue();
            issue2.setId("RFI-2026-038");
            issue2.setTitle("Curtain Wall Anchor Embed Depth");
            issue2.setStatus("IN REVIEW");
            issue2.setPriority("MEDIUM");
            issue2.setSiteLocation("Building 2 • Level 12 West Facade");
            RfiChatBottomSheet sheet = RfiChatBottomSheet.newInstance(issue2);
            sheet.show(getParentFragmentManager(), "RfiChatBottomSheet");
        });
    }

    private DailySiteReport buildReportFromInputs(String status) {
        String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        String weather = etDailyWeather.getText() != null ? etDailyWeather.getText().toString().trim() : "Clear";
        String work = etDailyCompletedWork.getText() != null ? etDailyCompletedWork.getText().toString().trim() : "";
        String manpower = etDailyManpower.getText() != null ? etDailyManpower.getText().toString().trim() : "";
        String materials = etDailyMaterials.getText() != null ? etDailyMaterials.getText().toString().trim() : "";
        String equipment = etDailyEquipment.getText() != null ? etDailyEquipment.getText().toString().trim() : "";

        return new DailySiteReport(
                "DSR-" + System.currentTimeMillis(),
                "Metro Central Tower",
                dateStr,
                "Elena Rostova (Site Engineer)",
                status,
                weather,
                work,
                manpower,
                materials,
                equipment,
                "None reported.",
                "",
                "Pour Level 14 perimeter columns and install edge protection.",
                dateStr,
                "Submitted".equalsIgnoreCase(status) ? dateStr : ""
        );
    }

    private void setupVoiceDictation() {
        voiceManager = new VoiceDictationManager(requireContext());
        btnVoiceDictateWork.setOnClickListener(v -> {
            if (!isListening) {
                btnVoiceDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate_active);
                isListening = true;
                voiceManager.startListening(new VoiceDictationManager.OnDictationListener() {
                    @Override
                    public void onSpeechRmsChanged(float rmsdB) {}

                    @Override
                    public void onPartialTranscription(String partialText) {
                        etDailyCompletedWork.setText(partialText);
                    }

                    @Override
                    public void onFinalTranscription(String finalText, File audioBackupFile) {
                        etDailyCompletedWork.setText(finalText);
                        btnVoiceDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }

                    @Override
                    public void onError(String userFriendlyError) {
                        btnVoiceDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate);
                        isListening = false;
                    }
                });
            } else {
                voiceManager.stopListening();
                btnVoiceDictateWork.setBackgroundResource(R.drawable.btn_mic_dictate);
                isListening = false;
            }
        });
    }

    @Override
    public void onDestroy() {
        if (voiceManager != null) {
            voiceManager.destroy();
        }
        super.onDestroy();
    }
}
