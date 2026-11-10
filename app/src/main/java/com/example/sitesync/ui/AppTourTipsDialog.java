package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.sitesync.R;
import com.example.sitesync.databinding.DialogAppTourTipsBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

public class AppTourTipsDialog extends DialogFragment {

    private DialogAppTourTipsBinding binding;
    private int currentStep = 0;

    private static class TipSlide {
        final int iconRes;
        final String title;
        final String description;
        final String proTip;

        TipSlide(int iconRes, String title, String description, String proTip) {
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
            this.proTip = proTip;
        }
    }

    private final TipSlide[] slides = new TipSlide[]{
            new TipSlide(
                    R.drawable.ic_camera,
                    "Multimodal Challan Vision OCR",
                    "Scan printed or handwritten Bangladesh delivery challans and cash memos with Gemini & Groq Vision. Quantities, units, and rates are automatically parsed and verified against live market prices.",
                    "Pro Tip: Prevents duplicate invoices and highlights vendor price inflation against Bangladesh market benchmarks."
            ),
            new TipSlide(
                    R.drawable.ic_straighten,
                    "Smart Building Scope & AI Progress",
                    "Set up building specifications (number of rooms, wall heights, doors, windows, and tiles). When you log daily site work, AI automatically computes % completion across BD civil trades.",
                    "Pro Tip: Simply write what was accomplished today—the AI surveyor figures out the completed percentages."
            ),
            new TipSlide(
                    R.drawable.ic_location,
                    "Geotagged Site Evidence & Cloud Storage",
                    "Capture tamper-proof site inspection photos embedded with GPS coordinates, elevation, timestamp, and active project code. Photos are instantly archived to Firebase Cloud Storage.",
                    "Pro Tip: Use the evidence camera to document concrete pours, rebar placement, and Thai glass installations."
            ),
            new TipSlide(
                    R.drawable.ic_people,
                    "Site Worker & Management Chat Room",
                    "Real-time communication room powered by Firebase Firestore and Realtime DB. Facilitates immediate coordination between project managers, engineers, supervisors, and site mistris.",
                    "Pro Tip: Post safety alerts or task instructions that workers can instantly view and acknowledge."
            ),
            new TipSlide(
                    R.drawable.ic_warning_hazard,
                    "Multi-Sensor Jobsite Safety & Digital Level",
                    "Utilize real-time acoustic microphone telemetry to monitor dangerous noise levels (>85 dB), 3-axis accelerometer for fall/impact detection, and digital spirit level for pipe slope grading.",
                    "Pro Tip: Keep the app active during perimeter work to automatically trigger emergency alerts on sudden impact."
            ),
            new TipSlide(
                    R.drawable.ic_checklist,
                    "Smart Worker Attendance & Tasks",
                    "Site workers can clock in and out with GPS-verified geolocation. Workers view assigned trade checklists (rod binding, plaster, tiles) and report completed work in everyday construction language.",
                    "Pro Tip: Managers can review and approve pending worker join requests directly from the project menu."
            )
    };

    public static AppTourTipsDialog newInstance() {
        return new AppTourTipsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAppTourTipsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        renderCurrentSlide();

        binding.btnCloseTipsDialog.setOnClickListener(v -> dismiss());
        binding.btnSkipTips.setOnClickListener(v -> dismiss());

        binding.btnNextTip.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            if (currentStep < slides.length - 1) {
                currentStep++;
                renderCurrentSlide();
            } else {
                dismiss();
            }
        });
    }

    private void renderCurrentSlide() {
        TipSlide slide = slides[currentStep];
        binding.tvTipStepPill.setText("Feature Guide • " + (currentStep + 1) + " of " + slides.length);
        binding.ivTipIcon.setImageResource(slide.iconRes);
        binding.tvTipTitle.setText(slide.title);
        binding.tvTipDescription.setText(slide.description);
        binding.tvTipProNote.setText(slide.proTip);

        if (currentStep == slides.length - 1) {
            binding.btnNextTip.setText("Get Started");
        } else {
            binding.btnNextTip.setText("Next Tip");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
