package com.example.sitesync.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.entities.ProjectEntity;
import com.example.sitesync.data.repository.ProjectRepository;
import com.example.sitesync.databinding.ActivityProjectHubBinding;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.List;

public class ProjectHubActivity extends AppCompatActivity implements ProjectCardAdapter.OnProjectClickListener {

    private ActivityProjectHubBinding binding;
    private ProjectRepository projectRepository;
    private SessionManager sessionManager;
    private ProjectCardAdapter adapter;
    private boolean isShowingAll = false;
    private LiveData<List<ProjectEntity>> currentProjectsLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityProjectHubBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        projectRepository = ProjectRepository.getInstance(this);

        setupUserInfo();
        setupRecyclerView();
        setupActions();

        // Seed realistic BD projects on cold start if empty
        projectRepository.seedDefaultProjectsIfEmpty(new ProjectRepository.ActionCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean seeded) {
                runOnUiThread(() -> observeProjects());
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> observeProjects());
            }
        });
    }

    private void setupUserInfo() {
        User user = sessionManager.getCurrentUser();
        boolean isWorker = sessionManager.isWorkerRole();
        if (user != null) {
            String roleName = isWorker ? "SITE WORKER" : (user.getRole() != null ? user.getRole() : "Manager");
            String fullName = user.getFullName() != null && !user.getFullName().trim().isEmpty() ? user.getFullName() : "User";
            String firstName = fullName.contains(" ") ? fullName.split(" ")[0] : fullName;
            binding.tvHubUserGreeting.setText(fullName + " • " + roleName);
            binding.tvHubWelcomeTitle.setText(isWorker ? ("Worker Hub • " + firstName) : ("Welcome back, " + firstName));
        } else {
            binding.tvHubUserGreeting.setText("SiteSync • Active Session");
            binding.tvHubWelcomeTitle.setText("Welcome to Hub");
        }

        if (isWorker) {
            binding.btnCreateNewProjectHub.setText("+ JOIN PROJECT WITH CODE");
            binding.btnToggleShowAllProjects.setVisibility(View.GONE);
            binding.tvRecentProjectsHeader.setText("Your Authorized Projects");
            adapter.setAllowOptions(false);
        } else {
            binding.btnCreateNewProjectHub.setText("+ CREATE NEW PROJECT");
            binding.btnToggleShowAllProjects.setVisibility(View.VISIBLE);
            binding.tvRecentProjectsHeader.setText(isShowingAll ? "All Tracked Projects" : "Recently Opened Projects");
            adapter.setAllowOptions(true);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProjectCardAdapter(this, this);
        adapter.setAllowOptions(!sessionManager.isWorkerRole());
        binding.rvRecentProjects.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentProjects.setAdapter(adapter);
    }

    private void setupActions() {
        boolean isWorker = sessionManager.isWorkerRole();

        // Create New Project (Manager) or Join Project with Code (Worker)
        binding.btnCreateNewProjectHub.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            if (isWorker) {
                promptJoinProjectWithCode();
            } else {
                ProjectPlanBottomSheetDialog dialog = ProjectPlanBottomSheetDialog.newInstance(null);
                dialog.setOnProjectSavedListener(project -> {
                    selectAndLaunchProject(project);
                });
                dialog.show(getSupportFragmentManager(), "CreateProjectDialog");
            }
        });

        // Toggle Show All vs Top 5 (Manager only)
        binding.btnToggleShowAllProjects.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            isShowingAll = !isShowingAll;
            binding.btnToggleShowAllProjects.setText(isShowingAll ? "Top 5" : "Show All");
            binding.tvRecentProjectsHeader.setText(isShowingAll ? "All Tracked Projects" : "Recently Opened Projects");
            observeProjects();
        });

        // Sign Out
        binding.btnHubSignOut.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            sessionManager.clearSession();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void promptJoinProjectWithCode() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g. MCT-2026 or PRJ-DHAKA-101");
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Join Project with Code")
                .setMessage("Enter the project code assigned to you by your site manager:")
                .setView(input)
                .setPositiveButton("Authorize", (dialog, which) -> {
                    String code = input.getText().toString().trim().toUpperCase(java.util.Locale.US);
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter a valid project code", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    projectRepository.getProjectById(code, new ProjectRepository.ActionCallback<ProjectEntity>() {
                        @Override
                        public void onSuccess(ProjectEntity project) {
                            runOnUiThread(() -> {
                                if (project != null) {
                                    sessionManager.addWorkerProjectCode(project.getProjectCode());
                                    // Record worker join request in database
                                    User user = sessionManager.getCurrentUser();
                                    String uid = user != null ? user.getUid() : "worker_" + System.currentTimeMillis();
                                    String name = user != null ? user.getFullName() : "Site Worker";
                                    String phone = user != null ? user.getPhoneNumber() : "";
                                    com.example.sitesync.data.local.entities.WorkerJoinRequestEntity req =
                                            new com.example.sitesync.data.local.entities.WorkerJoinRequestEntity(
                                                    java.util.UUID.randomUUID().toString(), uid, name, phone,
                                                    "General Worker", project.getProjectCode(), "PENDING", System.currentTimeMillis()
                                            );
                                    com.example.sitesync.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                                        com.example.sitesync.data.local.AppDatabase.getInstance(ProjectHubActivity.this)
                                                .workerJoinRequestDao().insert(req);
                                    });

                                    Toast.makeText(ProjectHubActivity.this, "Authorized for [" + project.getName() + "]!", Toast.LENGTH_SHORT).show();
                                    observeProjects();
                                    selectAndLaunchProject(project);
                                } else {
                                    Toast.makeText(ProjectHubActivity.this, "Project code '" + code + "' not found. Check with your manager.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onError(Exception error) {
                            runOnUiThread(() -> Toast.makeText(ProjectHubActivity.this, "Error looking up project code", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void observeProjects() {
        if (currentProjectsLiveData != null) {
            currentProjectsLiveData.removeObservers(this);
        }

        if (sessionManager.isWorkerRole()) {
            java.util.Set<String> workerCodes = new java.util.HashSet<>(sessionManager.getWorkerProjectCodes());
            String activeCode = sessionManager.getActiveProjectCode();
            if (activeCode != null && !activeCode.isEmpty()) {
                workerCodes.add(activeCode.toUpperCase(java.util.Locale.US));
            }

            if (workerCodes.isEmpty()) {
                binding.layoutEmptyProjects.setVisibility(View.VISIBLE);
                binding.rvRecentProjects.setVisibility(View.GONE);
                adapter.setProjects(null);
                return;
            }

            currentProjectsLiveData = projectRepository.getProjectsByCodesLive(new java.util.ArrayList<>(workerCodes));
        } else {
            if (isShowingAll) {
                currentProjectsLiveData = projectRepository.getAllProjectsLive();
            } else {
                currentProjectsLiveData = projectRepository.getRecentProjectsLive(5);
            }
        }

        currentProjectsLiveData.observe(this, projects -> {
            if (projects == null || projects.isEmpty()) {
                binding.layoutEmptyProjects.setVisibility(View.VISIBLE);
                binding.rvRecentProjects.setVisibility(View.GONE);
                adapter.setProjects(null);
            } else {
                binding.layoutEmptyProjects.setVisibility(View.GONE);
                binding.rvRecentProjects.setVisibility(View.VISIBLE);
                adapter.setProjects(projects);
            }
        });
    }

    @Override
    public void onProjectSelected(ProjectEntity project) {
        selectAndLaunchProject(project);
    }

    private void selectAndLaunchProject(ProjectEntity project) {
        sessionManager.setActiveProject(project.getId(), project.getProjectCode(), project.getName());
        projectRepository.recordProjectOpened(project.getId());

        Intent intent;
        if (sessionManager.isWorkerRole()) {
            intent = new Intent(this, WorkerDashboardActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.putExtra("project_code", project.getProjectCode());
        intent.putExtra("project_name", project.getName());
        intent.putExtra("show_tips", true);
        startActivity(intent);
    }

    @Override
    public void onProjectRemoved(ProjectEntity project) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Project")
                .setMessage("Are you sure you want to remove [" + project.getName() + "] from your recent project list?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    HapticFeedbackUtil.confirm(ProjectHubActivity.this);
                    projectRepository.deleteProject(project.getId(), new ProjectRepository.ActionCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            runOnUiThread(() ->
                                    Toast.makeText(ProjectHubActivity.this, "Project removed", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onError(Exception error) {
                            runOnUiThread(() ->
                                    Toast.makeText(ProjectHubActivity.this, "Could not remove project", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
