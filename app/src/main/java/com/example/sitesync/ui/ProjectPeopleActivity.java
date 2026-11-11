package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.WorkerJoinRequestEntity;
import com.example.sitesync.databinding.ActivityProjectPeopleBinding;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectPeopleActivity extends AppCompatActivity implements ProjectPeopleAdapter.OnPersonActionListener {

    private ActivityProjectPeopleBinding binding;
    private SessionManager sessionManager;
    private ProjectPeopleAdapter adapter;
    private String projectCode;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectPeopleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        projectCode = sessionManager.getActiveProjectCode();
        if (projectCode == null || projectCode.isEmpty()) projectCode = "PRJ-DHAKA-101";
        projectName = sessionManager.getActiveProjectName();
        if (projectName == null || projectName.isEmpty()) projectName = "Metro Central Tower";

        binding.tvPeopleProjectSubtitle.setText(projectName + " • " + projectCode);

        setupRecycler();
        setupListeners();
        seedDefaultPersonnelIfEmpty();
        loadPersonnel();
    }

    private void setupRecycler() {
        adapter = new ProjectPeopleAdapter(this, this);
        binding.rvProjectPeople.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProjectPeople.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackPeople.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            finish();
        });

        binding.btnAddMember.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showAddWorkerDialog();
        });

        binding.btnViewAttendanceTable.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            startActivity(new Intent(this, AttendanceTableActivity.class));
        });

        binding.btnPendingApprovals.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            WorkerRequestsBottomSheetDialog.newInstance(projectCode)
                    .show(getSupportFragmentManager(), "WorkerRequestsDialog");
        });
    }

    private void seedDefaultPersonnelIfEmpty() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<WorkerJoinRequestEntity> existing = db.workerJoinRequestDao().getRequestsForProject(projectCode);
            if (existing == null || existing.isEmpty()) {
                long now = System.currentTimeMillis();
                long day = 86400000L;

                List<WorkerJoinRequestEntity> seed = new ArrayList<>();
                seed.add(new WorkerJoinRequestEntity(
                        "REQ-01", "worker_rafiq", "Md. Rafiqul Islam", "+880 1712-345678",
                        "Lead Rod Binder", projectCode, "APPROVED", now - (5 * day)
                ));
                seed.add(new WorkerJoinRequestEntity(
                        "REQ-02", "worker_habib", "Habib Mistry", "+880 1819-223344",
                        "Rebar Fabricator", projectCode, "APPROVED", now - (4 * day)
                ));
                seed.add(new WorkerJoinRequestEntity(
                        "REQ-03", "worker_jalal", "Jalal Uddin", "+880 1911-556677",
                        "Head Raj Mistry", projectCode, "APPROVED", now - (3 * day)
                ));

                for (WorkerJoinRequestEntity req : seed) {
                    db.workerJoinRequestDao().insert(req);
                }
            }
        });
    }

    private void loadPersonnel() {
        AppDatabase.getInstance(this).workerJoinRequestDao()
                .getRequestsForProjectLive(projectCode)
                .observe(this, requests -> {
                    List<ProjectPeopleAdapter.ProjectPerson> list = new ArrayList<>();

                    // 1. Project Manager (from session or default)
                    User currentUser = sessionManager.getCurrentUser();
                    String mgrName = currentUser != null ? currentUser.getFullName() : "Marcus Vance";
                    String mgrPhone = (currentUser != null && currentUser.getPhoneNumber() != null) ? currentUser.getPhoneNumber() : "+880 1711-000001";
                    list.add(new ProjectPeopleAdapter.ProjectPerson(
                            "mgr_01", mgrName, "PROJECT_MANAGER", mgrPhone, "Authorized PM", true, false
                    ));

                    // 2. Site Supervisor
                    list.add(new ProjectPeopleAdapter.ProjectPerson(
                            "sup_01", "Alex Chen", "SITE_SUPERVISOR", "+880 1711-000002", "Lead Structural Supervisor", true, false
                    ));

                    // 3. Registered Workers
                    int workerCount = 0;
                    int pendingCount = 0;
                    if (requests != null) {
                        for (WorkerJoinRequestEntity req : requests) {
                            if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                                pendingCount++;
                            }
                            if ("APPROVED".equalsIgnoreCase(req.getStatus())) {
                                workerCount++;
                                list.add(new ProjectPeopleAdapter.ProjectPerson(
                                        req.getId(),
                                        req.getWorkerName() != null ? req.getWorkerName() : "Site Worker",
                                        "WORKER",
                                        req.getWorkerPhone() != null ? req.getWorkerPhone() : "+880 1700-000000",
                                        req.getWorkerTrade() != null ? req.getWorkerTrade() : "General Worker",
                                        true, // active
                                        true  // Manager can kick out workers
                                ));
                            }
                        }
                    }

                    if (pendingCount > 0) {
                        binding.btnPendingApprovals.setVisibility(View.VISIBLE);
                        binding.btnPendingApprovals.setText("Review Pending Join Requests (" + pendingCount + ")");
                    } else {
                        binding.btnPendingApprovals.setVisibility(View.GONE);
                    }

                    adapter.setPeople(list);

                    // Update Metrics
                    binding.tvMetricTotalPeople.setText(list.size() + " Members");
                    binding.tvMetricWorkersCount.setText(workerCount + " Workers");
                    binding.tvMetricLeadsCount.setText("2 Leads");

                    if (list.isEmpty()) {
                        binding.layoutEmptyPeople.setVisibility(View.VISIBLE);
                        binding.rvProjectPeople.setVisibility(View.GONE);
                    } else {
                        binding.layoutEmptyPeople.setVisibility(View.GONE);
                        binding.rvProjectPeople.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onKickOut(ProjectPeopleAdapter.ProjectPerson person) {
        HapticFeedbackUtil.heavyClick(this, null);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sign_out_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvSignOutDialogTitle);
        TextView tvMsg = dialogView.findViewById(R.id.tvSignOutDialogMessage);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelSignOut);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmSignOut);

        tvTitle.setText("Kick Out Worker?");
        tvMsg.setText("Are you sure you want to remove " + person.fullName + " from " + projectName + "? They will lose all access to site tasks, attendance, and team chat.");
        btnConfirm.setText("Kick Out");
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            HapticFeedbackUtil.scanSuccess(this);
            dialog.dismiss();

            // Execute Kick Out in database
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(ProjectPeopleActivity.this);
                // Mark request as REMOVED or delete
                db.workerJoinRequestDao().updateStatus(person.id, "REMOVED");

                runOnUiThread(() -> {
                    Toast.makeText(ProjectPeopleActivity.this, person.fullName + " has been removed from this project.", Toast.LENGTH_SHORT).show();
                });
            });
        });

        dialog.show();
    }

    private void showAddWorkerDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etName = new EditText(this);
        etName.setHint("Worker Full Name (e.g. Abdur Rahim)");
        layout.addView(etName);

        EditText etPhone = new EditText(this);
        etPhone.setHint("Mobile Number (e.g. +880 1711-223344)");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        EditText etTrade = new EditText(this);
        etTrade.setHint("Trade / Role (e.g. Mason / Raj Mistry)");
        layout.addView(etTrade);

        new AlertDialog.Builder(this)
                .setTitle("Add Worker to Project")
                .setMessage("Authorize a new site worker for " + projectName + " (" + projectCode + "):")
                .setView(layout)
                .setPositiveButton("Add Worker", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String trade = etTrade.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter worker name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WorkerJoinRequestEntity req = new WorkerJoinRequestEntity(
                            UUID.randomUUID().toString(),
                            "worker_" + System.currentTimeMillis(),
                            name,
                            phone.isEmpty() ? "+880 1700-000000" : phone,
                            trade.isEmpty() ? "General Site Worker" : trade,
                            projectCode,
                            "APPROVED",
                            System.currentTimeMillis()
                    );

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase.getInstance(this).workerJoinRequestDao().insert(req);
                        runOnUiThread(() -> {
                            HapticFeedbackUtil.confirm(ProjectPeopleActivity.this);
                            Toast.makeText(ProjectPeopleActivity.this, name + " added to " + projectCode + "!", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
