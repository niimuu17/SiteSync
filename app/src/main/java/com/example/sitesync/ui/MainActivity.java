package com.example.sitesync.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.sync.SyncManager;
import com.example.sitesync.databinding.ActivityMainBinding;
import com.example.sitesync.databinding.DialogCreateProjectBinding;
import com.example.sitesync.databinding.DialogProjectDetailsBinding;
import com.example.sitesync.databinding.DialogSignOutConfirmBinding;
import com.example.sitesync.databinding.DialogUserProfileInfoBinding;
import com.example.sitesync.hardware.BarometerElevationManager;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.NetworkMonitor;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import com.example.sitesync.hardware.UserLocationManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Screen 3: Main Dashboard (Default: Progress Tab)
 * Hosts the Global App Header with 3-dot overflow project actions,
 * a 5-tab Bottom Navigation dock, and swaps between modular fragments.
 */
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private BarometerElevationManager barometerElevationManager;
    private String currentProjectTitle = "Metro Central Tower";
    private int currentPendingCount = 0;
    private boolean isCurrentlyOnline = true;
    private AlertDialog activeProjectDialog;
    private ActivityResultLauncher<String> profilePhotoPickerLauncher;
    private ActivityResultLauncher<String[]> appPermissionsLauncher;
    private DialogUserProfileInfoBinding activeProfileDialogBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        appPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (UserLocationManager.hasLocationPermission(this)) {
                        UserLocationManager.getInstance(this).fetchCurrentLocation(loc -> {});
                        UserLocationManager.getInstance(this).startPassiveLocationUpdates();
                    }
                }
        );

        if (!UserLocationManager.hasLocationPermission(this) ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            appPermissionsLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
            });
        } else {
            UserLocationManager.getInstance(this).startPassiveLocationUpdates();
        }

        profilePhotoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        saveAndApplyProfilePhoto(uri);
                    }
                }
        );

        String activePName = sessionManager.getActiveProjectName();
        if (activePName != null && !activePName.isEmpty()) {
            currentProjectTitle = activePName;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        barometerElevationManager = new BarometerElevationManager(this);

        setupTopAppBar();
        setupBottomNavigation(savedInstanceState);

        // Show floating tour tips window on every app launch as requested (unless skip_tips is passed)
        boolean skipTips = getIntent() != null && getIntent().getBooleanExtra("skip_tips", false);
        if (!skipTips) {
            AppTourTipsDialog.newInstance().show(getSupportFragmentManager(), "AppTourTipsDialog");
        }

        if (getIntent() != null && "worker_requests".equals(getIntent().getStringExtra("open_dialog"))) {
            showWorkerRequestsDialog();
        }

        handleTabIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePhotoIntoViews();

        if (sessionManager.isFloatingVoiceAlerterEnabled()) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(this)) {
                com.example.sitesync.hardware.FloatingVoiceAlerterService.startAlerter(this);
            }
        }

        if (barometerElevationManager != null) {
            barometerElevationManager.startListening(new BarometerElevationManager.OnFloorLevelChangeListener() {
                @Override
                public void onFloorChanged(int computedFloor, float elevationDeltaMeters, float currentPressureHpa) {
                    binding.tvProjectSubtitle.setText(String.format(java.util.Locale.US, "Floor %d (Auto)", computedFloor));
                }

                @Override
                public void onRawElevationUpdate(float elevationDeltaMeters, float pressureHpa) {
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barometerElevationManager != null) {
            barometerElevationManager.stopListening();
        }
    }

    private void setupTopAppBar() {
        binding.tvProjectTitle.setText(currentProjectTitle);
        binding.tvProjectSubtitle.setText("Floor 3 (Auto)");
        binding.tvProjectSubtitle.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            int floor = barometerElevationManager != null ? barometerElevationManager.getActiveFloor() : 3;
            Toast.makeText(this, "Floor " + floor + " (Sensor Auto-Detect)", Toast.LENGTH_SHORT).show();
        });

        binding.btnNotificationsHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showWorkerRequestsDialog();
        });

        // Site Crew Chat Room Button (Manager access)
        binding.btnChatHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            Intent chatIntent = new Intent(this, SiteChatActivity.class);
            chatIntent.putExtra("project_code", sessionManager.getActiveProjectCode());
            chatIntent.putExtra("project_name", sessionManager.getActiveProjectName());
            startActivity(chatIntent);
        });

        // People Directory & Worker Management (Kick Out / Remove Workers)
        binding.btnPeopleHeader.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            Intent peopleIntent = new Intent(this, ProjectPeopleActivity.class);
            startActivity(peopleIntent);
        });

        // Observe pending worker clearance requests to toggle alert badge dot
        String activeCode = sessionManager.getActiveProjectCode();
        if (activeCode == null || activeCode.isEmpty()) {
            activeCode = "PRJ-DHAKA-101";
        }
        AppDatabase.getInstance(this).workerJoinRequestDao()
                .getPendingRequestsForProjectLive(activeCode)
                .observe(this, list -> {
                    int pending = (list != null) ? list.size() : 0;
                    if (binding.dotNotificationAlert != null) {
                        binding.dotNotificationAlert.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
                    }
                });

        // Clickable Brand Project Badge (Project Switcher & Options)
        binding.layoutProjectBadge.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showTopProjectMenu(v);
        });

        // Top Right Profile Picture (User Info Dialog)
        binding.btnUserProfileAvatar.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showUserProfileModal();
        });
    }

    private void showTopProjectMenu(android.view.View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.top_project_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_switch_project) {
                startActivity(new Intent(this, ProjectHubActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_site_chat) {
                Intent chatIntent = new Intent(this, SiteChatActivity.class);
                chatIntent.putExtra("project_code", sessionManager.getActiveProjectCode());
                chatIntent.putExtra("project_name", sessionManager.getActiveProjectName());
                startActivity(chatIntent);
                return true;
            } else if (id == R.id.menu_create_project) {
                ProjectPlanBottomSheetDialog planDialog = ProjectPlanBottomSheetDialog.newInstance(null);
                planDialog.setOnProjectSavedListener(project -> {
                    sessionManager.setActiveProject(project.getId(), project.getProjectCode(), project.getName());
                    currentProjectTitle = project.getName();
                    binding.tvProjectTitle.setText(currentProjectTitle);
                    Toast.makeText(this, "Created & Switched to: " + project.getName(), Toast.LENGTH_SHORT).show();
                });
                planDialog.show(getSupportFragmentManager(), "CreateProjectDialog");
                return true;
            } else if (id == R.id.menu_worker_requests) {
                showWorkerRequestsDialog();
                return true;
            } else if (id == R.id.menu_project_details) {
                showProjectDetailsModal();
                return true;
            } else if (id == R.id.menu_log_out) {
                confirmSignOut();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showWorkerRequestsDialog() {
        String activeCode = sessionManager.getActiveProjectCode();
        if (activeCode == null || activeCode.isEmpty()) {
            activeCode = "PRJ-DHAKA-101";
        }
        WorkerRequestsBottomSheetDialog dialog = WorkerRequestsBottomSheetDialog.newInstance(activeCode);
        dialog.show(getSupportFragmentManager(), "WorkerRequestsDialog");
    }

    private void showUserProfileModal() {
        DialogUserProfileInfoBinding dialogBinding = DialogUserProfileInfoBinding.inflate(getLayoutInflater());
        activeProfileDialogBinding = dialogBinding;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setOnDismissListener(d -> activeProfileDialogBinding = null);

        User user = sessionManager.getCurrentUser();
        if (user != null) {
            dialogBinding.tvProfileModalName.setText(user.getFullName());
            dialogBinding.tvProfileModalRole.setText(user.getRole() != null ? user.getRole() : "PROJECT MANAGER");
            dialogBinding.tvProfileModalEmail.setText(user.getEmail() != null ? user.getEmail() : user.getUsername());
        }
        dialogBinding.tvProfileModalProject.setText(sessionManager.getActiveProjectCode() + " • " + currentProjectTitle);

        String path = sessionManager.getProfilePhotoPath();
        if (path != null && new File(path).exists()) {
            dialogBinding.ivProfileModalAvatar.setImageTintList(null);
            dialogBinding.ivProfileModalAvatar.setImageURI(Uri.fromFile(new File(path)));
        }

        dialogBinding.btnChangeProfilePhoto.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            profilePhotoPickerLauncher.launch("image/*");
        });

        dialogBinding.btnProfileSwitchHub.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
            startActivity(new Intent(this, ProjectHubActivity.class));
            finish();
        });

        dialogBinding.btnProfileSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
            confirmSignOut();
        });

        dialogBinding.btnProfileClose.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        showCustomDialog(dialog);
    }

    private void saveAndApplyProfilePhoto(Uri uri) {
        try {
            File photoFile = new File(getFilesDir(), "manager_profile_photo.jpg");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(photoFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            sessionManager.setProfilePhotoPath(photoFile.getAbsolutePath());
            loadProfilePhotoIntoViews();
            Toast.makeText(this, "Profile photo updated successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to update profile photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePhotoIntoViews() {
        String path = sessionManager.getProfilePhotoPath();
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Uri fileUri = Uri.fromFile(file);
                binding.ivUserAvatarHeader.setImageTintList(null);
                binding.ivUserAvatarHeader.setImageURI(null);
                binding.ivUserAvatarHeader.setImageURI(fileUri);
                if (activeProfileDialogBinding != null) {
                    activeProfileDialogBinding.ivProfileModalAvatar.setImageTintList(null);
                    activeProfileDialogBinding.ivProfileModalAvatar.setImageURI(null);
                    activeProfileDialogBinding.ivProfileModalAvatar.setImageURI(fileUri);
                }
                return;
            }
        }
        binding.ivUserAvatarHeader.setImageResource(R.drawable.ic_person);
        binding.ivUserAvatarHeader.setImageTintList(getColorStateList(R.color.forest_surface_deep));
    }

    private void showCreateProjectDialog() {
        DialogCreateProjectBinding dialogBinding = DialogCreateProjectBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogBinding.btnCloseCreateProject.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        dialogBinding.btnCancelCreateProject.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        dialogBinding.btnSubmitCreateProject.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            String name = dialogBinding.etProjectName.getText().toString().trim();
            String code = dialogBinding.etProjectCode.getText().toString().trim();
            if (name.isEmpty()) {
                dialogBinding.etProjectName.setError("Project name required");
                return;
            }
            currentProjectTitle = name;
            binding.tvProjectTitle.setText(currentProjectTitle);
            String codePrefix = !code.isEmpty() ? "[" + code + "] " : "";
            Toast.makeText(this, "Active Project: " + codePrefix + name, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        showCustomDialog(dialog);
    }

    private void showProjectDetailsModal() {
        DialogProjectDetailsBinding dialogBinding = DialogProjectDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogBinding.tvProjectDetailsTitle.setText(currentProjectTitle);

        int currentFloor = barometerElevationManager != null ? barometerElevationManager.getActiveFloor() : 3;
        dialogBinding.tvDetailFloor.setText(String.format(Locale.US, "Level %d (Perimeter Concrete Columns)", currentFloor));

        dialogBinding.btnCloseProjectDetails.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        dialogBinding.btnDoneProjectDetails.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        showCustomDialog(dialog);
    }

    private void confirmSignOut() {
        DialogSignOutConfirmBinding dialogBinding = DialogSignOutConfirmBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogBinding.btnCancelSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            dialog.dismiss();
        });

        dialogBinding.btnConfirmSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            sessionManager.clearSession();
            redirectToLogin();
        });

        showCustomDialog(dialog);
    }

    private void showCustomDialog(AlertDialog dialog) {
        if (activeProjectDialog != null && activeProjectDialog.isShowing()) {
            activeProjectDialog.dismiss();
        }
        activeProjectDialog = dialog;
        activeProjectDialog.show();
    }

    private void setupBottomNavigation(Bundle savedInstanceState) {
        binding.bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            if (getIntent() != null && (getIntent().hasExtra("tab") || getIntent().hasExtra("dialog"))) {
                handleTabIntent(getIntent());
            } else {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_progress);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTabIntent(intent);
    }

    public void selectNavigationTab(int itemId) {
        if (binding != null && binding.bottomNavigationView != null) {
            binding.bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    private void handleTabIntent(Intent intent) {
        if (intent == null || binding == null) return;
        String tab = intent.getStringExtra("tab");
        if (tab != null) {
            if ("progress".equalsIgnoreCase(tab)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_progress);
            } else if ("task".equalsIgnoreCase(tab)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_task);
            } else if ("resource".equalsIgnoreCase(tab)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_resource);
            } else if ("security".equalsIgnoreCase(tab)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_security);
            } else if ("settings".equalsIgnoreCase(tab)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_settings);
            }
        }

        String openDialog = intent.getStringExtra("dialog");
        if (openDialog != null) {
            if ("create_project".equalsIgnoreCase(openDialog)) {
                showCreateProjectDialog();
            } else if ("user_profile".equalsIgnoreCase(openDialog)) {
                showUserProfileModal();
            } else if ("project_details".equalsIgnoreCase(openDialog)) {
                showProjectDetailsModal();
            } else if ("sign_out".equalsIgnoreCase(openDialog)) {
                confirmSignOut();
            } else if ("review_challan".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_resource);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current instanceof ResourceFragment) {
                        ((ResourceFragment) current).openDemoChallanReview();
                    }
                }, 400);
            } else if ("invalid_receipt_warning".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_resource);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current instanceof ResourceFragment) {
                        ((ResourceFragment) current).showInvalidReceiptWarningDialog(
                                "The scanned image does not contain a delivery challan, cash memo, or receipt. (Pre-Validation Gate Triggered)"
                        );
                    }
                }, 400);
            } else if ("duplicate_receipt_warning".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_resource);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current instanceof ResourceFragment) {
                        com.example.sitesync.hardware.ChallanOcrVisionClient.ParsedChallan sample = new com.example.sitesync.hardware.ChallanOcrVisionClient.ParsedChallan();
                        sample.challanNumber = "CH-94812";
                        sample.supplierName = "Meghna Cement & Steel Mills Ltd";
                        com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult dup =
                                com.example.sitesync.data.repository.InventoryRepository.DuplicateCheckResult.duplicate(
                                        "Challan #CH-94812 from Meghna Cement was already entered into inventory on Nov 10, 2026.",
                                        "CH-94812", "Meghna Cement & Steel Mills Ltd", System.currentTimeMillis() - 3600000L, 500400.0);
                        ((ResourceFragment) current).showDuplicateReceiptWarningDialog(sample, dup);
                    }
                }, 400);
            } else if ("stock_bar_chart".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_resource);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current instanceof ResourceFragment) {
                        ((ResourceFragment) current).showCompleteStockBarChart();
                    }
                }, 800);
            } else if ("evidence_dump".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_progress);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    SiteEvidenceBottomSheetDialog dialog = SiteEvidenceBottomSheetDialog.newInstance();
                    dialog.show(getSupportFragmentManager(), "SiteEvidenceDialog");
                }, 400);
            } else if ("fall_alert".equalsIgnoreCase(openDialog)) {
                binding.bottomNavigationView.setSelectedItemId(R.id.nav_security);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    com.example.sitesync.hardware.FallAlertCountdownDialog.newInstance(3.6f)
                            .show(getSupportFragmentManager(), "FallAlertCountdownDialog");
                }, 400);
            }
        }

        int scrollY = intent.getIntExtra("scroll_y", 0);
        if (scrollY > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                if (current != null && current.getView() != null) {
                    androidx.core.widget.NestedScrollView nsv = findNestedScrollView(current.getView());
                    if (nsv != null) {
                        nsv.smoothScrollTo(0, scrollY);
                    }
                }
            }, 350);
        }
    }

    private androidx.core.widget.NestedScrollView findNestedScrollView(View view) {
        if (view instanceof androidx.core.widget.NestedScrollView) {
            return (androidx.core.widget.NestedScrollView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                androidx.core.widget.NestedScrollView nsv = findNestedScrollView(vg.getChildAt(i));
                if (nsv != null) return nsv;
            }
        }
        return null;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_progress) {
            selectedFragment = ProgressFragment.newInstance();
        } else if (id == R.id.nav_task) {
            selectedFragment = TaskFragment.newInstance();
        } else if (id == R.id.nav_resource) {
            selectedFragment = ResourceFragment.newInstance();
        } else if (id == R.id.nav_security) {
            selectedFragment = SecurityFragment.newInstance();
        } else if (id == R.id.nav_settings) {
            selectedFragment = SettingsFragment.newInstance();
        }

        if (selectedFragment != null) {
            HapticFeedbackUtil.lightClick(this, null);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            UserLocationManager.getInstance(this).stopPassiveLocationUpdates();
        } catch (Exception ignored) {}
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
