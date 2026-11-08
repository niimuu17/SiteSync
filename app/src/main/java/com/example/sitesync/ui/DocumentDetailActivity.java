package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.DocumentRepository;
import com.example.sitesync.databinding.ActivityDocumentDetailBinding;
import com.example.sitesync.models.DocumentRevision;
import com.example.sitesync.models.SiteDocument;

/**
 * Screen displaying complete drawing specifications, latest approved revision status,
 * and a chronological revision audit timeline.
 */
public class DocumentDetailActivity extends AppCompatActivity implements DocumentRevisionAdapter.OnRevisionActionListener {

    private ActivityDocumentDetailBinding binding;
    private DocumentRepository repository;
    private DocumentRevisionAdapter revisionAdapter;
    private String docId;
    private SiteDocument currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DocumentRepository(this);
        docId = getIntent().getStringExtra(DocumentManagementActivity.EXTRA_DOC_ID);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocumentDetails();
    }

    private void initViews() {
        revisionAdapter = new DocumentRevisionAdapter(this);
        binding.rvRevisionHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRevisionHistory.setAdapter(revisionAdapter);
    }

    private void setupListeners() {
        binding.btnBackFromDocDetail.setOnClickListener(v -> finish());

        // Latest Revision Actions
        binding.btnViewLatestPdf.setOnClickListener(v -> {
            if (currentDocument != null) {
                Intent intent = new Intent(DocumentDetailActivity.this, PdfViewerActivity.class);
                intent.putExtra(DocumentManagementActivity.EXTRA_DOC_ID, currentDocument.getId());
                startActivity(intent);
            }
        });

        binding.btnDownloadLatestPdf.setOnClickListener(v -> {
            if (currentDocument != null) {
                Toast.makeText(this, "Downloading active revision " + currentDocument.getCurrentRevision() + "...", Toast.LENGTH_SHORT).show();
            }
        });

        // Add new revision button
        binding.btnUploadNewRevision.setOnClickListener(v -> {
            if (currentDocument != null) {
                Intent intent = new Intent(DocumentDetailActivity.this, UploadDocumentActivity.class);
                intent.putExtra(DocumentManagementActivity.EXTRA_DOC_ID, currentDocument.getId());
                startActivity(intent);
            }
        });
    }

    private void loadDocumentDetails() {
        currentDocument = repository.getDocumentById(docId);
        if (currentDocument == null) {
            finish();
            return;
        }

        binding.tvDocDetailIdSubtitle.setText(currentDocument.getId() + " • Revision Audit History");
        binding.tvDetailDocName.setText(currentDocument.getName());
        binding.tvDetailProject.setText(currentDocument.getProjectName() + " • Lead: " + currentDocument.getUploadedBy());
        binding.tvDetailDescription.setText(currentDocument.getDescription());

        // Category Badge
        applyCategoryStyle(currentDocument.getCategory());

        // Latest Revision Header
        DocumentRevision latestApproved = currentDocument.getLatestApprovedRevision();
        if (latestApproved != null) {
            binding.tvLatestRevisionHeader.setText("Active On-Site Version: " + latestApproved.getRevisionNumber());
            binding.tvLatestRevisionNotes.setText("Approved: " + latestApproved.getChangeNotes());
        } else {
            binding.tvLatestRevisionHeader.setText("Current Version: " + currentDocument.getCurrentRevision());
            binding.tvLatestRevisionNotes.setText("Pending official approval stamp.");
        }

        // Populate Revisions Timeline
        revisionAdapter.setRevisions(currentDocument.getRevisionHistory());
    }

    private void applyCategoryStyle(String category) {
        binding.tvDetailCategoryBadge.setText(category);

        if (SiteDocument.CATEGORY_ARCHITECTURAL.equalsIgnoreCase(category)) {
            binding.tvDetailCategoryBadge.setBackgroundColor(Color.parseColor("#231F3D"));
            binding.tvDetailCategoryBadge.setTextColor(Color.parseColor("#A78BFA"));
        } else if (SiteDocument.CATEGORY_STRUCTURAL.equalsIgnoreCase(category)) {
            binding.tvDetailCategoryBadge.setBackgroundColor(Color.parseColor("#1E293B"));
            binding.tvDetailCategoryBadge.setTextColor(Color.parseColor("#38BDF8"));
        } else if (SiteDocument.CATEGORY_ELECTRICAL.equalsIgnoreCase(category)) {
            binding.tvDetailCategoryBadge.setBackgroundColor(Color.parseColor("#382B14"));
            binding.tvDetailCategoryBadge.setTextColor(Color.parseColor("#F59E0B"));
        } else if (SiteDocument.CATEGORY_PLUMBING.equalsIgnoreCase(category)) {
            binding.tvDetailCategoryBadge.setBackgroundColor(Color.parseColor("#132E2E"));
            binding.tvDetailCategoryBadge.setTextColor(Color.parseColor("#2DD4BF"));
        }
    }

    @Override
    public void onViewRevision(DocumentRevision revision) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra(DocumentManagementActivity.EXTRA_DOC_ID, currentDocument.getId());
        intent.putExtra(PdfViewerActivity.EXTRA_REVISION_NUMBER, revision.getRevisionNumber());
        startActivity(intent);
    }

    @Override
    public void onDownloadRevision(DocumentRevision revision) {
        Toast.makeText(this, "Downloading revision " + revision.getRevisionNumber() + "...", Toast.LENGTH_SHORT).show();
    }
}
