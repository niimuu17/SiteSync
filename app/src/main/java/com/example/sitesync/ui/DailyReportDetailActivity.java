package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.DailyReportRepository;
import com.example.sitesync.data.PdfReportGenerator;
import com.example.sitesync.databinding.ActivityDailyReportDetailBinding;
import com.example.sitesync.models.DailySiteReport;

/**
 * Screen displaying full Daily Site Report details, resources, problems, next-day tasks,
 * photo evidence, and one-tap PDF generation and sharing with the Project Manager.
 */
public class DailyReportDetailActivity extends AppCompatActivity {

    private ActivityDailyReportDetailBinding binding;
    private DailyReportRepository repository;
    private String reportId;
    private DailySiteReport currentReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDailyReportDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DailyReportRepository(this);
        reportId = getIntent().getStringExtra(DailyReportListActivity.EXTRA_REPORT_ID);

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportDetails();
    }

    private void setupListeners() {
        binding.btnBackFromReportDetail.setOnClickListener(v -> finish());

        // Header PDF Share
        binding.btnHeaderSharePdf.setOnClickListener(v -> sharePdf());

        // Bottom PDF Share
        binding.btnSharePdfWithPM.setOnClickListener(v -> sharePdf());

        // Edit Report Button
        binding.btnEditReport.setOnClickListener(v -> {
            if (currentReport != null) {
                Intent intent = new Intent(DailyReportDetailActivity.this, CreateDailyReportActivity.class);
                intent.putExtra(CreateDailyReportActivity.EXTRA_EDIT_REPORT_ID, currentReport.getId());
                startActivity(intent);
            }
        });
    }

    private void loadReportDetails() {
        if (reportId == null) {
            Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentReport = repository.getReportById(reportId);
        if (currentReport == null) {
            Toast.makeText(this, "Report not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvDetailReportId.setText(currentReport.getId());
        binding.tvDetailDate.setText(currentReport.getReportDate());
        binding.tvDetailProject.setText(currentReport.getProjectName());
        binding.tvDetailPreparedBy.setText(currentReport.getPreparedBy());
        binding.tvDetailWeather.setText(currentReport.getWeather());
        binding.tvDetailCompletedWork.setText(currentReport.getCompletedWork());
        binding.tvDetailManpower.setText(currentReport.getManpower());
        binding.tvDetailMaterials.setText(currentReport.getMaterialsUsed());
        binding.tvDetailEquipment.setText(currentReport.getEquipment());
        binding.tvDetailSiteProblems.setText(currentReport.getSiteProblems());
        binding.tvDetailNextDayPlans.setText(currentReport.getNextDayPlans());

        // Status badge styling
        if (currentReport.isSubmitted()) {
            binding.tvDetailStatusBadge.setText("Submitted");
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#123324"));
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#10B981"));
        } else {
            binding.tvDetailStatusBadge.setText("Draft");
            binding.tvDetailStatusBadge.setBackgroundColor(Color.parseColor("#382B14"));
            binding.tvDetailStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
        }

        // Photo Attachment handling
        if (currentReport.getPhotoUri() != null && !currentReport.getPhotoUri().isEmpty()) {
            binding.cardReportPhoto.setVisibility(View.VISIBLE);
            binding.tvDetailNoPhotoNotice.setVisibility(View.GONE);
            binding.ivDetailReportPhoto.setVisibility(View.VISIBLE);
            try {
                binding.ivDetailReportPhoto.setImageURI(Uri.parse(currentReport.getPhotoUri()));
            } catch (Exception e) {
                binding.ivDetailReportPhoto.setVisibility(View.GONE);
                binding.tvDetailNoPhotoNotice.setText("Unable to preview attached image");
                binding.tvDetailNoPhotoNotice.setVisibility(View.VISIBLE);
            }
        } else {
            binding.ivDetailReportPhoto.setVisibility(View.GONE);
            binding.tvDetailNoPhotoNotice.setVisibility(View.VISIBLE);
        }
    }

    private void sharePdf() {
        if (currentReport != null) {
            PdfReportGenerator.shareReportPdf(this, currentReport);
        }
    }
}
