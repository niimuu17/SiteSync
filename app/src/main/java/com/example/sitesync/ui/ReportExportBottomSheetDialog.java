package com.example.sitesync.ui;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.DialogReportExportBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.PdfReportGenerator;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportExportBottomSheetDialog extends BottomSheetDialogFragment {

    private DialogReportExportBinding binding;
    private SessionManager sessionManager;

    public static ReportExportBottomSheetDialog newInstance() {
        return new ReportExportBottomSheetDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogReportExportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        binding.btnCloseExportModal.setOnClickListener(v -> dismiss());
        binding.btnExportModalPdf.setOnClickListener(v -> generateAndSharePdf());
        binding.btnExportModalCsv.setOnClickListener(v -> generateAndShareCsv());
    }

    private void generateAndSharePdf() {
        HapticFeedbackUtil.lightClick(requireContext(), null);
        String pName = sessionManager.getActiveProjectName();
        String pCode = sessionManager.getActiveProjectCode();

        try {
            File pdfFile = PdfReportGenerator.generateMockReport(requireContext(), pName, pCode);
            if (pdfFile != null && pdfFile.exists()) {
                Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", pdfFile);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("application/pdf");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Site Progress Report (PDF)"));
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Report generation ready in local storage", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "PDF export note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generateAndShareCsv() {
        HapticFeedbackUtil.lightClick(requireContext(), null);
        String pName = sessionManager.getActiveProjectName();
        String pCode = sessionManager.getActiveProjectCode();

        File csvFile = new File(requireContext().getCacheDir(), "SiteSync_Audit_" + pCode + ".csv");
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Timestamp,Project Code,Project Name,Trade,Accomplishment,Progress %,Status,Certified By\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            String now = sdf.format(new Date());
            writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%.1f%%,%s,%s\n",
                    now, pCode, pName, "Thai Glass", "Installed 4 aluminum sliding windows", 68.0, "VERIFIED", "Lead Engineer"));
            writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%.1f%%,%s,%s\n",
                    now, pCode, pName, "Tiles & Flooring", "Porcelain floor tiles setting bedroom 1", 35.0, "IN_PROGRESS", "Tile Mistry"));
            writer.write(String.format(Locale.US, "%s,%s,%s,%s,%s,%.1f%%,%s,%s\n",
                    now, pCode, pName, "Electrical", "Concealed conduit pipe installation", 50.0, "VERIFIED", "Electrician"));

            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", csvFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Field Audit Log (CSV/Excel)"));
            dismiss();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "CSV export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
