package com.example.sitesync.ui.cockpit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.repository.UnifiedProjectRepository;
import com.example.sitesync.hardware.CheckLevelDialog;
import com.example.sitesync.ui.MainActivity;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.ui.custom.SemiCircleGaugeView;
import com.example.sitesync.ui.dialogs.UpdateProgressBottomSheet;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

/**
 * Tab 1: Work Areas & Discipline Telemetry
 * High-contrast industrial 2x2 grid representing Foundation, Structural, Electrical, and Finishing.
 * Connects directly to hardware inclinometer and barcode scanners.
 */
public class WorkAreasTabFragment extends Fragment {

    private View cardFoundation;
    private View cardStructural;
    private View cardElectrical;
    private View cardFinishing;

    private SemiCircleGaugeView gaugeFoundation;
    private SemiCircleGaugeView gaugeStructural;
    private SemiCircleGaugeView gaugeElectrical;
    private SemiCircleGaugeView gaugeFinishing;

    private TextView tvActiveWorkersCount;
    private MaterialButton btnQuickCheckLevel;
    private MaterialButton btnQuickScanTicket;

    private UnifiedProjectRepository repository;

    public static WorkAreasTabFragment newInstance() {
        return new WorkAreasTabFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_work_areas_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = UnifiedProjectRepository.getInstance(requireContext());

        cardFoundation = view.findViewById(R.id.cardFoundation);
        cardStructural = view.findViewById(R.id.cardStructural);
        cardElectrical = view.findViewById(R.id.cardElectrical);
        cardFinishing = view.findViewById(R.id.cardFinishing);

        gaugeFoundation = view.findViewById(R.id.gaugeFoundation);
        gaugeStructural = view.findViewById(R.id.gaugeStructural);
        gaugeElectrical = view.findViewById(R.id.gaugeElectrical);
        gaugeFinishing = view.findViewById(R.id.gaugeFinishing);

        tvActiveWorkersCount = view.findViewById(R.id.tvActiveWorkersCount);
        btnQuickCheckLevel = view.findViewById(R.id.btnQuickCheckLevel);
        btnQuickScanTicket = view.findViewById(R.id.btnQuickScanTicket);

        // Click handlers on discipline cards to fast-update progress
        cardFoundation.setOnClickListener(v -> openDisciplineUpdate("Foundation Pour", 85, gaugeFoundation));
        cardStructural.setOnClickListener(v -> openDisciplineUpdate("Structural Work", 62, gaugeStructural));
        cardElectrical.setOnClickListener(v -> openDisciplineUpdate("Electrical Conduits", 40, gaugeElectrical));
        cardFinishing.setOnClickListener(v -> openDisciplineUpdate("Interior Finishing", 18, gaugeFinishing));

        // Quick Hardware Tool: Check Level
        btnQuickCheckLevel.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            CheckLevelDialog dialog = new CheckLevelDialog();
            dialog.setOnMeasurementSavedListener(measurement -> {
                Toast.makeText(requireContext(), "Grade Verified: " + measurement, Toast.LENGTH_SHORT).show();
            });
            dialog.show(getParentFragmentManager(), "CheckLevelDialog");
        });

        // Quick Hardware Tool: Scan Delivery Ticket
        btnQuickScanTicket.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            Toast.makeText(requireContext(), "Navigating to Delivery Challan Scanner...", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectNavigationTab(R.id.nav_resource);
            }
        });
    }

    private void openDisciplineUpdate(String name, int currentProg, SemiCircleGaugeView gaugeView) {
        HapticFeedbackUtil.lightClick(requireContext(), null);

        ConstructionActivity act = new ConstructionActivity();
        act.setId("DISC-" + name.replaceAll("\\s+", "").toUpperCase());
        act.setName(name);
        act.setProgress(currentProg);
        act.setStatus("IN PROGRESS");

        UpdateProgressBottomSheet sheet = UpdateProgressBottomSheet.newInstance(act);
        sheet.setOnProgressUpdatedListener((newProgress, status, notes) -> {
            if (gaugeView != null) {
                gaugeView.setProgress(newProgress);
            }
        });
        sheet.show(getParentFragmentManager(), "UpdateProgressBottomSheet");
    }
}
