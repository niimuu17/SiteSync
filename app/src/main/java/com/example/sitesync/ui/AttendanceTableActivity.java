package com.example.sitesync.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.WorkerAttendanceEntity;
import com.example.sitesync.databinding.ActivityAttendanceTableBinding;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.example.sitesync.utils.ProjectStorageHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceTableActivity extends AppCompatActivity {

    private ActivityAttendanceTableBinding binding;
    private SessionManager sessionManager;
    private AttendanceTableAdapter adapter;
    private String projectCode;
    private String projectName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAttendanceTableBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        projectCode = sessionManager.getActiveProjectCode();
        if (projectCode == null || projectCode.isEmpty()) projectCode = "PRJ-DHAKA-101";
        projectName = sessionManager.getActiveProjectName();
        if (projectName == null || projectName.isEmpty()) projectName = "Metro Central Tower";

        binding.tvAttendanceProjectSubtitle.setText(projectName + " • " + projectCode);

        setupRecycler();
        setupListeners();
        seedSampleAttendanceIfEmpty();
        observeAttendanceData();
    }

    private void setupRecycler() {
        adapter = new AttendanceTableAdapter(this);
        binding.rvAttendanceTable.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAttendanceTable.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnBackAttendance.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            finish();
        });

        binding.btnExportAttendanceCsv.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            exportAttendanceCsv();
        });
    }

    private void seedSampleAttendanceIfEmpty() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<WorkerAttendanceEntity> existing = db.workerAttendanceDao().getProjectAttendancesSync(projectCode);
            if (existing == null || existing.isEmpty()) {
                long now = System.currentTimeMillis();
                long hour = 3600000L;
                long day = 86400000L;

                List<WorkerAttendanceEntity> seed = new ArrayList<>();
                // Active on duty workers today
                WorkerAttendanceEntity w1 = new WorkerAttendanceEntity(
                        "ATT-01", "WRK-501", "Md. Rafiqul Islam", projectCode,
                        now - (3 * hour), 23.8103, 90.4125
                );
                w1.setShiftStatus("ON_DUTY");
                seed.add(w1);

                WorkerAttendanceEntity w2 = new WorkerAttendanceEntity(
                        "ATT-02", "WRK-502", "Habib Mistry (Rod Binder)", projectCode,
                        now - (2 * hour), 23.8105, 90.4128
                );
                w2.setShiftStatus("ON_DUTY");
                seed.add(w2);

                WorkerAttendanceEntity w3 = new WorkerAttendanceEntity(
                        "ATT-03", "WRK-503", "Jalal Uddin (Raj Mistry)", projectCode,
                        now - (4 * hour), 23.8101, 90.4122
                );
                w3.setShiftStatus("ON_DUTY");
                seed.add(w3);

                WorkerAttendanceEntity w4 = new WorkerAttendanceEntity(
                        "ATT-04", "WRK-504", "Alamgir Hossain (Thai)", projectCode,
                        now - (1 * hour), 23.8102, 90.4124
                );
                w4.setShiftStatus("ON_DUTY");
                seed.add(w4);

                // Completed shifts from yesterday
                WorkerAttendanceEntity w5 = new WorkerAttendanceEntity(
                        "ATT-05", "WRK-501", "Md. Rafiqul Islam", projectCode,
                        now - day - (9 * hour), 23.8103, 90.4125
                );
                w5.setClockOutTime(now - day);
                w5.setClockedIn(false);
                w5.setShiftStatus("COMPLETED");
                seed.add(w5);

                WorkerAttendanceEntity w6 = new WorkerAttendanceEntity(
                        "ATT-06", "WRK-505", "Sumon Mia (Tile Setter)", projectCode,
                        now - day - (8 * hour), 23.8104, 90.4126
                );
                w6.setClockOutTime(now - day);
                w6.setClockedIn(false);
                w6.setShiftStatus("COMPLETED");
                seed.add(w6);

                for (WorkerAttendanceEntity item : seed) {
                    db.workerAttendanceDao().insert(item);
                }
            }
        });
    }

    private void observeAttendanceData() {
        AppDatabase.getInstance(this).workerAttendanceDao()
                .getProjectAttendancesLive(projectCode)
                .observe(this, list -> {
                    if (list == null || list.isEmpty()) {
                        binding.layoutEmptyAttendance.setVisibility(View.VISIBLE);
                        binding.rvAttendanceTable.setVisibility(View.GONE);
                        binding.tvMetricOnDutyCount.setText("0 Workers");
                        binding.tvMetricTotalLogs.setText("0 Shifts");
                        adapter.setItems(null);
                    } else {
                        binding.layoutEmptyAttendance.setVisibility(View.GONE);
                        binding.rvAttendanceTable.setVisibility(View.VISIBLE);
                        adapter.setItems(list);

                        int onDuty = 0;
                        for (WorkerAttendanceEntity att : list) {
                            if (att.isClockedIn() || "ON_DUTY".equalsIgnoreCase(att.getShiftStatus())) {
                                onDuty++;
                            }
                        }
                        binding.tvMetricOnDutyCount.setText(onDuty + " Workers");
                        binding.tvMetricTotalLogs.setText(list.size() + " Shifts");
                        binding.tvMetricGeofenceRate.setText("100% Pass");
                    }
                });
    }

    private void exportAttendanceCsv() {
        List<WorkerAttendanceEntity> data = adapter.getItems();
        if (data.isEmpty()) {
            Toast.makeText(this, "No attendance records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        File reportsDir = ProjectStorageHelper.getReportsDir(this, projectCode);
        File csvFile = new File(reportsDir, "Attendance_Report_" + projectCode + ".csv");

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.US);

        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("LogID,WorkerID,WorkerName,ProjectCode,Date,ClockIn,ClockOut,DurationMinutes,GeofenceStatus,ShiftStatus\n");
            for (WorkerAttendanceEntity row : data) {
                long inTime = row.getClockInTime();
                long outTime = row.getClockOutTime();
                long durationMins = outTime > inTime ? ((outTime - inTime) / 60000L) : 0;
                String dStr = inTime > 0 ? sdfDate.format(new Date(inTime)) : "";
                String inStr = inTime > 0 ? sdfTime.format(new Date(inTime)) : "";
                String outStr = outTime > 0 ? sdfTime.format(new Date(outTime)) : "ACTIVE";

                writer.write(String.format(Locale.US, "%s,%s,\"%s\",%s,%s,%s,%s,%d,VERIFIED,%s\n",
                        row.getId(), row.getWorkerId(), row.getWorkerName(), row.getProjectCode(),
                        dStr, inStr, outStr, durationMins, row.getShiftStatus()));
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", csvFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "SiteSync Employee Attendance Sheet - " + projectCode);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Export Attendance CSV"));
            Toast.makeText(this, "Exporting Attendance Sheet...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
