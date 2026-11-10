package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.dao.WorkerJoinRequestDao;
import com.example.sitesync.data.local.entities.WorkerJoinRequestEntity;
import com.example.sitesync.databinding.DialogWorkerRequestsBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.UUID;

public class WorkerRequestsBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_PROJECT_CODE = "arg_project_code";

    private DialogWorkerRequestsBinding binding;
    private WorkerRequestsAdapter adapter;
    private String projectCode;
    private WorkerJoinRequestDao workerJoinRequestDao;

    public static WorkerRequestsBottomSheetDialog newInstance(String projectCode) {
        WorkerRequestsBottomSheetDialog dialog = new WorkerRequestsBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_CODE, projectCode);
        dialog.setArguments(args);
        return dialog;
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
        binding = DialogWorkerRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectCode = getArguments().getString(ARG_PROJECT_CODE);
        }
        if (projectCode == null || projectCode.isEmpty()) {
            SessionManager sessionManager = new SessionManager(requireContext());
            projectCode = sessionManager.getActiveProjectCode();
        }
        if (projectCode == null || projectCode.isEmpty()) {
            projectCode = "PRJ-DHAKA-101";
        }

        binding.tvProjectCodeBadge.setText(projectCode);
        binding.btnCloseWorkerRequests.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            dismiss();
        });

        workerJoinRequestDao = AppDatabase.getInstance(requireContext()).workerJoinRequestDao();

        setupRecyclerView();
        observeRequests();
    }

    private void setupRecyclerView() {
        adapter = new WorkerRequestsAdapter(requireContext(), new WorkerRequestsAdapter.OnWorkerRequestActionListener() {
            @Override
            public void onApprove(WorkerJoinRequestEntity request) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    workerJoinRequestDao.updateStatus(request.getId(), "APPROVED", System.currentTimeMillis());
                });
                Toast.makeText(requireContext(), "Approved " + request.getWorkerName() + " for site access!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(WorkerJoinRequestEntity request) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    workerJoinRequestDao.updateStatus(request.getId(), "REJECTED", System.currentTimeMillis());
                });
                Toast.makeText(requireContext(), "Rejected request for " + request.getWorkerName(), Toast.LENGTH_SHORT).show();
            }
        });

        binding.rvWorkerRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvWorkerRequests.setAdapter(adapter);
    }

    private void observeRequests() {
        workerJoinRequestDao.getRequestsForProjectLive(projectCode).observe(getViewLifecycleOwner(), requests -> {
            if (requests == null || requests.isEmpty()) {
                // If completely empty, auto-seed 2 realistic requests for this project
                seedDemoRequestsIfEmpty();
                binding.layoutEmptyRequests.setVisibility(View.GONE);
                binding.rvWorkerRequests.setVisibility(View.VISIBLE);
            } else {
                binding.layoutEmptyRequests.setVisibility(View.GONE);
                binding.rvWorkerRequests.setVisibility(View.VISIBLE);

                adapter.setItems(requests);

                int pendingCount = 0;
                for (WorkerJoinRequestEntity req : requests) {
                    if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                        pendingCount++;
                    }
                }

                if (pendingCount > 0) {
                    binding.tvPendingCount.setText(pendingCount + " PENDING CLEARANCE APPROVAL" + (pendingCount > 1 ? "S" : ""));
                } else {
                    binding.tvPendingCount.setText("ALL REQUESTS REVIEWED (" + requests.size() + " TOTAL)");
                }
            }
        });
    }

    private void seedDemoRequestsIfEmpty() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<WorkerJoinRequestEntity> existing = workerJoinRequestDao.getPendingRequestsForProject(projectCode);
            if (existing == null || existing.isEmpty()) {
                long now = System.currentTimeMillis();
                WorkerJoinRequestEntity req1 = new WorkerJoinRequestEntity(
                        UUID.randomUUID().toString(),
                        "worker_rafiq",
                        "Md. Rafiqul Islam",
                        "+880 1712-345678",
                        "Steel Fixer / Rebar Binder",
                        projectCode,
                        "PENDING",
                        now - (18 * 60 * 1000)
                );

                WorkerJoinRequestEntity req2 = new WorkerJoinRequestEntity(
                        UUID.randomUUID().toString(),
                        "worker_kalam",
                        "Abul Kalam Mistry",
                        "+880 1819-876543",
                        "Head Plaster Mistry",
                        projectCode,
                        "PENDING",
                        now - (45 * 60 * 1000)
                );

                workerJoinRequestDao.insert(req1);
                workerJoinRequestDao.insert(req2);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}