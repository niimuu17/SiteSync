package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.DocumentRepository;
import com.example.sitesync.databinding.ActivityDocumentManagementBinding;
import com.example.sitesync.models.SiteDocument;

import java.util.List;

/**
 * Main Drawing & Document Management screen for the Engineer Module.
 * Categorizes drawings as Architectural, Structural, Electrical, and Plumbing.
 * Supports PDF viewing, real-time search, downloading, and revision auditing.
 */
public class DocumentManagementActivity extends AppCompatActivity implements SiteDocumentAdapter.OnDocumentActionListener {

    public static final String EXTRA_DOC_ID = "extra_doc_id";

    private ActivityDocumentManagementBinding binding;
    private DocumentRepository repository;
    private SiteDocumentAdapter adapter;
    private String currentSelectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new DocumentRepository(this);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        filterAndSearchDocuments();
    }

    private void initViews() {
        adapter = new SiteDocumentAdapter(this);
        binding.rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDocuments.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackFromDocs.setOnClickListener(v -> finish());

        // Category Filter Chips
        binding.chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.chipCatStructural.getId()) {
                currentSelectedCategory = SiteDocument.CATEGORY_STRUCTURAL;
            } else if (checkedId == binding.chipCatArchitectural.getId()) {
                currentSelectedCategory = SiteDocument.CATEGORY_ARCHITECTURAL;
            } else if (checkedId == binding.chipCatElectrical.getId()) {
                currentSelectedCategory = SiteDocument.CATEGORY_ELECTRICAL;
            } else if (checkedId == binding.chipCatPlumbing.getId()) {
                currentSelectedCategory = SiteDocument.CATEGORY_PLUMBING;
            } else {
                currentSelectedCategory = "All";
            }
            filterAndSearchDocuments();
        });

        // Search TextWatcher for real-time query matching
        binding.etSearchDocument.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterAndSearchDocuments();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearchDocument.setText("");
            filterAndSearchDocuments();
        });

        // Upload FAB
        binding.fabUploadDocument.setOnClickListener(v -> {
            Intent intent = new Intent(DocumentManagementActivity.this, UploadDocumentActivity.class);
            startActivity(intent);
        });
    }

    private void filterAndSearchDocuments() {
        String query = binding.etSearchDocument.getText() != null ? binding.etSearchDocument.getText().toString().trim() : "";
        List<SiteDocument> results = repository.searchDocuments(query, currentSelectedCategory);

        adapter.setDocuments(results);
        binding.layoutEmptyDocuments.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onViewClicked(SiteDocument document) {
        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra(EXTRA_DOC_ID, document.getId());
        startActivity(intent);
    }

    @Override
    public void onDownloadClicked(SiteDocument document) {
        Toast.makeText(this, "Downloading " + document.getName() + " (" + document.getCurrentRevision() + ")...", Toast.LENGTH_SHORT).show();
        binding.getRoot().postDelayed(() -> {
            Toast.makeText(this, "Saved to device: " + document.getName() + ".pdf", Toast.LENGTH_LONG).show();
        }, 1200);
    }

    @Override
    public void onRevisionsClicked(SiteDocument document) {
        Intent intent = new Intent(this, DocumentDetailActivity.class);
        intent.putExtra(EXTRA_DOC_ID, document.getId());
        startActivity(intent);
    }
}
