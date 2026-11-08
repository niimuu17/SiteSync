package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.data.DocumentRepository;
import com.example.sitesync.data.InspectionRepository;
import com.example.sitesync.data.IssueRepository;
import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityEngineerDashboardBinding;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.DailySiteReport;
import com.example.sitesync.models.SiteDocument;
import com.example.sitesync.models.SiteInspection;
import com.example.sitesync.models.TechnicalIssue;
import com.example.sitesync.models.User;

import java.util.List;

public class EngineerDashboardActivity extends AppCompatActivity {

    private ActivityEngineerDashboardBinding binding;
    private SessionManager sessionManager;
    private ProgressRepository progressRepository;
    private InspectionRepository inspectionRepository;
    private DocumentRepository documentRepository;
    private IssueRepository issueRepository;
    private DailyReportRepository dailyReportRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEngineerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        progressRepository = new ProgressRepository(this);
        inspectionRepository = new InspectionRepository(this);
        documentRepository = new DocumentRepository(this);
        issueRepository = new IssueRepository(this);
        dailyReportRepository = new DailyReportRepository(this);

        User user = (User) getIntent().getSerializableExtra(AuthRouter.EXTRA_USER);
        if (user == null) {
            user = sessionManager.getCurrentUser();
        }

        if (user != null) {
            binding.tvEngineerName.setText(user.getFullName());
            binding.tvEngineerUserId.setText("User ID: " + user.getUsername());
        }

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProgressSummary();
        updateInspectionSummary();
        updateDocumentSummary();
        updateIssueSummary();
        updateDailyReportSummary();
    }

    private void setupListeners() {
        // Module 1: Site Progress Monitoring
        binding.cardProgressMonitoring.setOnClickListener(v -> openProgressMonitoring());
        binding.btnOpenProgressMonitor.setOnClickListener(v -> openProgressMonitoring());

        // Module 2: Site Inspection & Quality Control
        binding.cardQualityInspection.setOnClickListener(v -> openQualityControl());
        binding.btnOpenInspectionHub.setOnClickListener(v -> openQualityControl());

        // Module 3: Drawing & Document Management
        binding.cardDrawingManagement.setOnClickListener(v -> openDrawingManagement());
        binding.btnOpenDrawingHub.setOnClickListener(v -> openDrawingManagement());

        // Module 4: Technical Issues & RFIs
        binding.cardIssueManagement.setOnClickListener(v -> openIssueManagement());
        binding.btnOpenIssueHub.setOnClickListener(v -> openIssueManagement());

        // Module 5: Daily Site Reports
        binding.cardDailyReports.setOnClickListener(v -> openDailyReports());
        binding.btnOpenDailyReportHub.setOnClickListener(v -> openDailyReports());

        binding.btnSignOutEngineer.setOnClickListener(v -> confirmSignOut());
    }

    private void openProgressMonitoring() {
        Intent intent = new Intent(EngineerDashboardActivity.this, ProgressMonitoringActivity.class);
        startActivity(intent);
    }

    private void openQualityControl() {
        Intent intent = new Intent(EngineerDashboardActivity.this, InspectionListActivity.class);
        startActivity(intent);
    }

    private void openDrawingManagement() {
        Intent intent = new Intent(EngineerDashboardActivity.this, DocumentManagementActivity.class);
        startActivity(intent);
    }

    private void openIssueManagement() {
        Intent intent = new Intent(EngineerDashboardActivity.this, IssueManagementActivity.class);
        startActivity(intent);
    }

    private void openDailyReports() {
        Intent intent = new Intent(EngineerDashboardActivity.this, DailyReportListActivity.class);
        startActivity(intent);
    }

    private void updateProgressSummary() {
        List<ConstructionActivity> activities = progressRepository.getAllActivities();
        int inProgressCount = 0;
        for (ConstructionActivity act : activities) {
            if (ConstructionActivity.STATUS_IN_PROGRESS.equalsIgnoreCase(act.getStatus())) {
                inProgressCount++;
            }
        }
        binding.tvProgressBadgeText.setText(activities.size() + " Total Tasks • " + inProgressCount + " In Progress");
    }

    private void updateInspectionSummary() {
        List<SiteInspection> inspections = inspectionRepository.getAllInspections();
        int failedCount = 0;
        for (SiteInspection item : inspections) {
            if (SiteInspection.STATUS_FAILED.equalsIgnoreCase(item.getStatus())) {
                failedCount++;
            }
        }

        if (failedCount > 0) {
            binding.tvInspectionBadgeText.setText(inspections.size() + " Audits • " + failedCount + " Re-Inspection Needed");
        } else {
            binding.tvInspectionBadgeText.setText(inspections.size() + " Audits • All Compliant");
        }
    }

    private void updateDocumentSummary() {
        List<SiteDocument> docs = documentRepository.getAllDocuments();
        binding.tvDocumentBadgeText.setText(docs.size() + " CAD Blueprints • 4 Disciplines Synced");
    }

    private void updateIssueSummary() {
        List<TechnicalIssue> issues = issueRepository.getAllIssues();
        int highCount = 0;
        for (TechnicalIssue item : issues) {
            if (TechnicalIssue.PRIORITY_HIGH.equalsIgnoreCase(item.getPriority())) {
                highCount++;
            }
        }
        binding.tvIssueBadgeText.setText(issues.size() + " Active RFIs • " + highCount + " High Priority");
    }

    private void updateDailyReportSummary() {
        List<DailySiteReport> reports = dailyReportRepository.getAllReports();
        int submitted = 0;
        for (DailySiteReport r : reports) {
            if (r.isSubmitted()) {
                submitted++;
            }
        }
        binding.tvDailyReportBadgeText.setText(reports.size() + " Reports • " + submitted + " Submitted to PM");
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to exit the Field Engineer dashboard?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(EngineerDashboardActivity.this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
