package com.example.sitesync.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.example.sitesync.data.local.entities.ProjectEntity;
import com.example.sitesync.databinding.ItemProjectHubCardBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProjectCardAdapter extends RecyclerView.Adapter<ProjectCardAdapter.ProjectViewHolder> {

    public interface OnProjectClickListener {
        void onProjectSelected(ProjectEntity project);
        void onProjectRemoved(ProjectEntity project);
    }

    private final Context context;
    private final OnProjectClickListener listener;
    private final List<ProjectEntity> projects = new ArrayList<>();
    private boolean allowOptions = true;

    public ProjectCardAdapter(Context context, OnProjectClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setAllowOptions(boolean allowOptions) {
        this.allowOptions = allowOptions;
        notifyDataSetChanged();
    }

    public void setProjects(List<ProjectEntity> newProjects) {
        this.projects.clear();
        if (newProjects != null) {
            this.projects.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectHubCardBinding binding = ItemProjectHubCardBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        holder.bind(projects.get(position));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final ItemProjectHubCardBinding b;

        ProjectViewHolder(ItemProjectHubCardBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(ProjectEntity project) {
            b.tvHubProjectCode.setText(project.getProjectCode() != null ? project.getProjectCode() : "PRJ");
            b.tvHubProjectStatus.setText(project.getStatus() != null ? project.getStatus() : "ACTIVE");
            b.tvHubProjectTitle.setText(project.getName() != null ? project.getName() : "Site Project");

            String subtitle = (project.getClientName() != null ? project.getClientName() : "Client") +
                    " • " + (project.getLocation() != null ? project.getLocation() : "Bangladesh");
            b.tvHubProjectSubtitle.setText(subtitle);

            // Scope summary
            String scope = String.format(Locale.US, "Scope: %s • %.0fft Walls • %d Windows • %s",
                    project.getBuildingType() != null ? project.getBuildingType() : "Building",
                    project.getWallHeightFeet() > 0 ? project.getWallHeightFeet() : 10.0f,
                    project.getNumWindows() > 0 ? project.getNumWindows() : 4,
                    project.getFlooringType() != null ? project.getFlooringType() : "Tiles");
            b.tvHubProjectScopeSummary.setText(scope);

            int progress = Math.round(project.getOverallProgressPercent());
            b.pbHubProjectProgress.setProgress(Math.max(0, Math.min(100, progress)));
            b.tvHubProjectPercent.setText(progress + "%");

            // Card click to select active project
            itemView.setOnClickListener(v -> {
                HapticFeedbackUtil.lightClick(context, v);
                if (listener != null) {
                    listener.onProjectSelected(project);
                }
            });

            // Three-dot options menu (Manager only)
            b.btnHubProjectOptions.setVisibility(allowOptions ? View.VISIBLE : View.GONE);
            b.btnHubProjectOptions.setOnClickListener(v -> {
                HapticFeedbackUtil.lightClick(context, v);
                showOptionsMenu(v, project);
            });
        }

        private void showOptionsMenu(View anchor, ProjectEntity project) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenu().add(0, 1, 0, "Remove Project");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    if (listener != null) {
                        listener.onProjectRemoved(project);
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}
