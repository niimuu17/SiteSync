package com.example.sitesync.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.SiteEvidencePhotoEntity;
import com.example.sitesync.databinding.DialogSiteEvidenceGalleryBinding;
import com.example.sitesync.hardware.SitePhotoStorageManager;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SiteEvidenceBottomSheetDialog extends BottomSheetDialogFragment {

    private DialogSiteEvidenceGalleryBinding binding;
    private SitePhotoStorageManager storageManager;
    private SessionManager sessionManager;
    private Uri tempPhotoUri;
    private File tempPhotoFile;

    private final ActivityResultLauncher<Uri> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && tempPhotoFile != null && tempPhotoFile.exists()) {
                    processCapturedPhoto(tempPhotoFile);
                }
            });

    private final ActivityResultLauncher<String> galleryPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    copyUriToTempAndProcess(uri);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCaptureInternal();
                } else {
                    Toast.makeText(requireContext(), "Camera permission required to capture evidence photos", Toast.LENGTH_SHORT).show();
                }
            });

    public static SiteEvidenceBottomSheetDialog newInstance() {
        return new SiteEvidenceBottomSheetDialog();
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
        binding = DialogSiteEvidenceGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storageManager = new SitePhotoStorageManager(requireContext());
        sessionManager = new SessionManager(requireContext());

        loadRecentPhotos();

        binding.btnCloseEvidenceModal.setOnClickListener(v -> dismiss());
        binding.btnCaptureEvidenceModal.setOnClickListener(v -> launchCameraCapture());
        binding.btnAddProjectPhotoModal.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(requireContext(), v);
            galleryPhotoLauncher.launch("image/*");
        });
    }

    private void loadRecentPhotos() {
        String pCode = sessionManager.getActiveProjectCode();
        AppDatabase.getDatabase(requireContext()).siteEvidencePhotoDao()
                .getPhotosForProjectLive(pCode)
                .observe(getViewLifecycleOwner(), photos -> {
                    if (binding == null) return;
                    if (photos != null && !photos.isEmpty()) {
                        SiteEvidencePhotoEntity latest = photos.get(0);
                        binding.layoutEvidencePlaceholder.setVisibility(View.GONE);
                        binding.ivEvidenceMainPreview.setImageURI(Uri.fromFile(new File(latest.getLocalFilePath())));
                        binding.tvEvidenceGpsBadge.setText(latest.getLocationLabel() + " • Synced");
                        binding.tvEvidenceCounter.setText("Archived: " + photos.size() + " photos in Firebase Cloud Storage");
                    } else {
                        binding.layoutEvidencePlaceholder.setVisibility(View.VISIBLE);
                        binding.tvEvidenceCounter.setText("No photos archived for " + pCode);
                    }
                });
    }

    private void launchCameraCapture() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCameraCaptureInternal();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCaptureInternal() {
        try {
            tempPhotoFile = File.createTempFile("evidence_", ".jpg", requireContext().getCacheDir());
            tempPhotoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempPhotoFile
            );
            takePhotoLauncher.launch(tempPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Camera init error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyUriToTempAndProcess(Uri uri) {
        try {
            File dest = File.createTempFile("gallery_evidence_", ".jpg", requireContext().getCacheDir());
            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                if (in == null) return;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            processCapturedPhoto(dest);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Could not import photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processCapturedPhoto(File file) {
        String pCode = sessionManager.getActiveProjectCode();
        String engineer = sessionManager.getCurrentUser() != null ? sessionManager.getCurrentUser().getFullName() : "Engineer";
        double lat = 23.8103;
        double lng = 90.4125;
        double elev = 14.5;

        storageManager.processAndStoreEvidence(file, pCode, lat, lng, elev,
                "Routine structural inspection evidence", engineer,
                new SitePhotoStorageManager.OnPhotoStoredListener() {
                    @Override
                    public void onSuccess(SiteEvidencePhotoEntity photo) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            HapticFeedbackUtil.scanSuccess(requireContext());
                            Toast.makeText(requireContext(), "Photo Watermarked & Queued for Cloud Upload!", Toast.LENGTH_SHORT).show();
                            if (binding != null) {
                                binding.layoutEvidencePlaceholder.setVisibility(View.GONE);
                                binding.ivEvidenceMainPreview.setImageURI(Uri.fromFile(new File(photo.getLocalFilePath())));
                                binding.tvEvidenceGpsBadge.setText(photo.getLocationLabel() + " • Stamped");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Upload note: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
