package com.example.sitesync.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.ProgressRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityAddActivityBinding;
import com.example.sitesync.models.ConstructionActivity;
import com.example.sitesync.models.ProgressLog;
import com.example.sitesync.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen enabling engineers to log and schedule new construction activities.
 */
public class AddActivityActivity extends AppCompatActivity {

    private ActivityAddActivityBinding binding;
    private ProgressRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProgressRepository(this);
        sessionManager = new SessionManager(this);

        binding.btnBackFromAdd.setOnClickListener(v -> finish());
        binding.btnSubmitAddActivity.setOnClickListener(v -> handleCreateActivity());
    }

    private void handleCreateActivity() {
        String projectName = getText(binding.etAddProjectName);
        String activityName = getText(binding.etAddActivityName);
        String location = getText(binding.etAddLocation);
        String remarks = getText(binding.etAddRemarks);

        if (TextUtils.isEmpty(projectName)) {
            binding.tilAddProjectName.setError("Project name cannot be empty");
            return;
        } else {
            binding.tilAddProjectName.setError(null);
        }

        if (TextUtils.isEmpty(activityName)) {
            binding.tilAddActivityName.setError("Activity name cannot be empty");
            return;
        } else {
            binding.tilAddActivityName.setError(null);
        }

        if (TextUtils.isEmpty(location)) {
            binding.tilAddLocation.setError("Location cannot be empty");
            return;
        } else {
            binding.tilAddLocation.setError(null);
        }

        User currentUser = sessionManager.getCurrentUser();
        String engineerName = currentUser != null ? currentUser.getFullName() : "Field Engineer";
        String timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());

        String newId = "ACT-" + (100 + repository.getAllActivities().size() + 1);

        List<ProgressLog> history = new ArrayList<>();
        history.add(new ProgressLog(
                "L-" + System.currentTimeMillis(),
                0,
                ConstructionActivity.STATUS_PENDING,
                remarks.isEmpty() ? "Task scheduled for execution" : remarks,
                timestamp,
                engineerName,
                ""
        ));

        ConstructionActivity newActivity = new ConstructionActivity(
                newId,
                projectName,
                activityName,
                location,
                0,
                ConstructionActivity.STATUS_PENDING,
                timestamp,
                engineerName,
                remarks,
                "",
                history
        );

        repository.addActivity(newActivity);
        Toast.makeText(this, "Activity created: " + activityName, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
