package com.example.sitesync.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sitesync.R;
import com.google.android.material.button.MaterialButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProjectPeopleAdapter extends RecyclerView.Adapter<ProjectPeopleAdapter.ViewHolder> {

    public static class ProjectPerson implements Serializable {
        public String id;
        public String fullName;
        public String role; // "PROJECT_MANAGER", "SITE_SUPERVISOR", "WORKER"
        public String phone;
        public String tradeOrTitle;
        public boolean isOnline;
        public boolean canKickOut;

        public ProjectPerson(String id, String fullName, String role, String phone, String tradeOrTitle, boolean isOnline, boolean canKickOut) {
            this.id = id;
            this.fullName = fullName;
            this.role = role;
            this.phone = phone;
            this.tradeOrTitle = tradeOrTitle;
            this.isOnline = isOnline;
            this.canKickOut = canKickOut;
        }
    }

    public interface OnPersonActionListener {
        void onKickOut(ProjectPerson person);
    }

    private final Context context;
    private final List<ProjectPerson> people = new ArrayList<>();
    private final OnPersonActionListener listener;

    public ProjectPeopleAdapter(Context context, OnPersonActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setPeople(List<ProjectPerson> list) {
        people.clear();
        if (list != null) {
            people.addAll(list);
        }
        notifyDataSetChanged();
    }

    public List<ProjectPerson> getPeople() {
        return people;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project_person_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectPerson p = people.get(position);

        // Initials
        String initials = "SS";
        if (p.fullName != null && !p.fullName.trim().isEmpty()) {
            String[] parts = p.fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (parts[0].length() >= 2) {
                initials = parts[0].substring(0, 2).toUpperCase();
            } else {
                initials = parts[0].toUpperCase();
            }
        }
        holder.tvAvatarInitials.setText(initials);

        // Name
        holder.tvPersonName.setText(p.fullName);

        // Role & Badges
        String role = p.role != null ? p.role.toUpperCase() : "WORKER";
        if (role.contains("MANAGER")) {
            holder.tvRoleBadge.setText("PROJECT MANAGER");
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_medium);
            holder.tvRoleBadge.setTextColor(Color.parseColor("#92400E"));
            holder.btnKickOut.setVisibility(View.GONE);
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
            holder.tvAdminBadge.setText("Manager");
        } else if (role.contains("SUPERVISOR")) {
            holder.tvRoleBadge.setText("SITE SUPERVISOR");
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_green);
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_green_text));
            holder.btnKickOut.setVisibility(View.GONE);
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
            holder.tvAdminBadge.setText("Lead");
        } else {
            holder.tvRoleBadge.setText("SITE WORKER");
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_low);
            holder.tvRoleBadge.setTextColor(Color.parseColor("#1E293B"));
            holder.btnKickOut.setVisibility(p.canKickOut ? View.VISIBLE : View.GONE);
            holder.tvAdminBadge.setVisibility(View.GONE);
        }

        // Subtext (Phone & Trade)
        String sub = "";
        if (p.phone != null && !p.phone.isEmpty()) {
            sub += p.phone;
        }
        if (p.tradeOrTitle != null && !p.tradeOrTitle.isEmpty()) {
            sub += (sub.isEmpty() ? "" : " • ") + p.tradeOrTitle;
        }
        holder.tvSubtext.setText(sub.isEmpty() ? "Assigned to Site Tasks" : sub);

        // Online Dot
        holder.dotOnlineStatus.setVisibility(p.isOnline ? View.VISIBLE : View.GONE);

        // Kick Out Action
        holder.btnKickOut.setOnClickListener(v -> {
            if (listener != null) {
                listener.onKickOut(p);
            }
        });

        // Implicit Intent: Tap contact row to dial site personnel directly
        holder.itemView.setOnClickListener(v -> {
            if (p.phone != null && !p.phone.isEmpty()) {
                try {
                    android.content.Intent dialIntent = new android.content.Intent(
                            android.content.Intent.ACTION_DIAL,
                            android.net.Uri.parse("tel:" + p.phone.replaceAll("[^0-9+]", ""))
                    );
                    context.startActivity(dialIntent);
                } catch (Exception ignored) {}
            }
        });

        // Alternating row background
        if (position % 2 == 1) {
            holder.itemView.setBackgroundColor(Color.parseColor("#082B443E"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatarInitials;
        final View dotOnlineStatus;
        final TextView tvPersonName;
        final TextView tvRoleBadge;
        final TextView tvSubtext;
        final MaterialButton btnKickOut;
        final TextView tvAdminBadge;

        ViewHolder(View v) {
            super(v);
            tvAvatarInitials = v.findViewById(R.id.tvPersonAvatarInitials);
            dotOnlineStatus = v.findViewById(R.id.dotOnlineStatus);
            tvPersonName = v.findViewById(R.id.tvPersonName);
            tvRoleBadge = v.findViewById(R.id.tvPersonRoleBadge);
            tvSubtext = v.findViewById(R.id.tvPersonSubtext);
            btnKickOut = v.findViewById(R.id.btnKickOutWorker);
            tvAdminBadge = v.findViewById(R.id.tvAdminBadge);
        }
    }
}
