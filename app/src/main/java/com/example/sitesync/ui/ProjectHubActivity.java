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
        if (user != null) {
            String roleName = user.getRole() != null ? user.getRole() : "Manager";
            binding.tvHubUserGreeting.setText(user.getFullName() + " • " + roleName);
            binding.tvHubWelcomeTitle.setText("Welcome back, " + user.getFullName().split(" ")[0]);
        }
    }

    private void setupRecyclerView() {
        adapter = new ProjectCardAdapter(this, this);
        binding.rvRecentProjects.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentProjects.setAdapter(adapter);
    }

    private void setupActions() {
        // Create New Project Button (Prompts Mandatory Building Scope Plan)
        binding.btnCreateNewProjectHub.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            ProjectPlanBottomSheetDialog dialog = ProjectPlanBottomSheetDialog.newInstance(null);
            dialog.setOnProjectSavedListener(project -> {
                selectAndLaunchProject(project);
            });
            dialog.show(getSupportFragmentManager(), "CreateProjectDialog");
        });

        // Toggle Show All vs Top 5
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

    private void observeProjects() {
        if (currentProjectsLiveData != null) {
            currentProjectsLiveData.removeObservers(this);
        }

        if (isShowingAll) {
            currentProjectsLiveData = projectRepository.getAllProjectsLive();
        } else {
            currentProjectsLiveData = projectRepository.getRecentProjectsLive(5);
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
