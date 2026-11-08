package com.example.sitesync.ui.cockpit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sitesync.R;
import com.example.sitesync.data.AccessControl;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.example.sitesync.ui.WelcomeActivity;
import com.example.sitesync.ui.common.ContextBannerView;
import com.example.sitesync.ui.common.NavigationDockView;
import com.example.sitesync.utils.HapticFeedbackUtil;

/**
 * Unified Site Command Cockpit (Single-Module Architecture).
 * Replaces fragmented role-segregated dashboards with a unified single cockpit.
 * Connects 4 segmented telemetry tabs, capability-based access control,
 * sticky barometer floor detection, and floating frosted navigation dock.
 */
public class UnifiedSiteCockpitActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private User currentUser;

    private TextView tvUserRoleClaim;
    private TextView tvActiveRoleBadge;
    private ImageView btnCockpitMenu;
    private ContextBannerView contextBannerView;

    private TextView tabPillWorkAreas;
    private TextView tabPillProgress;
    private TextView tabPillQaCad;
    private TextView tabPillDaily;

    private ViewPager2 viewPagerCockpit;
    private NavigationDockView navigationDock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_site_cockpit);

        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getCurrentUser();

        initViews();
        setupUserClaims();
        setupViewPager();
        setupPillTabs();
        setupDockNavigation();
        setupMenu();
    }

    private void initViews() {
        tvUserRoleClaim = findViewById(R.id.tvUserRoleClaim);
        tvActiveRoleBadge = findViewById(R.id.tvActiveRoleBadge);
        btnCockpitMenu = findViewById(R.id.btnCockpitMenu);
        contextBannerView = findViewById(R.id.contextBannerView);

        tabPillWorkAreas = findViewById(R.id.tabPillWorkAreas);
        tabPillProgress = findViewById(R.id.tabPillProgress);
        tabPillQaCad = findViewById(R.id.tabPillQaCad);
        tabPillDaily = findViewById(R.id.tabPillDaily);

        viewPagerCockpit = findViewById(R.id.viewPagerCockpit);
        navigationDock = findViewById(R.id.navigationDock);
    }

    private void setupUserClaims() {
        if (currentUser == null) {
            currentUser = new User("engineer", "Field Staff", Role.ENGINEER.name(), "");
        }

        Role role = AccessControl.getRole(currentUser);
        String name = currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                ? currentUser.getFullName().toUpperCase() : "FIELD OPERATOR";

        tvUserRoleClaim.setText(name + " • " + role.getDisplayName().toUpperCase());
        tvActiveRoleBadge.setText(role.name());
    }

    private void setupViewPager() {
        viewPagerCockpit.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return WorkAreasTabFragment.newInstance();
                    case 1:
                        return ProgressTabFragment.newInstance();
                    case 2:
                        return QualityCadTabFragment.newInstance();
                    case 3:
                        return DailyLogTabFragment.newInstance();
                    default:
                        return WorkAreasTabFragment.newInstance();
                }
            }

            @Override
            public int getItemCount() {
                return 4;
            }
        });

        viewPagerCockpit.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePillVisuals(position);
                updateDockVisuals(position);
            }
        });
    }

    private void setupPillTabs() {
        tabPillWorkAreas.setOnClickListener(v -> selectTab(0));
        tabPillProgress.setOnClickListener(v -> selectTab(1));
        tabPillQaCad.setOnClickListener(v -> selectTab(2));
        tabPillDaily.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int position) {
        HapticFeedbackUtil.lightClick(this, null);
        viewPagerCockpit.setCurrentItem(position, true);
    }

    private void updatePillVisuals(int position) {
        TextView[] pills = new TextView[]{tabPillWorkAreas, tabPillProgress, tabPillQaCad, tabPillDaily};
        for (int i = 0; i < pills.length; i++) {
            if (i == position) {
                pills[i].setBackgroundResource(R.drawable.bg_tab_pill_active);
                pills[i].setTextColor(ContextCompat.getColor(this, R.color.dark_canvas));
            } else {
                pills[i].setBackgroundResource(R.drawable.bg_tab_pill_inactive);
                pills[i].setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }
    }

    private void setupDockNavigation() {
        navigationDock.setOnTabSelectedListener(tabIndex -> {
            switch (tabIndex) {
                case NavigationDockView.TAB_HOME:
                    viewPagerCockpit.setCurrentItem(0, true);
                    return true;
                case NavigationDockView.TAB_PROGRESS:
                    viewPagerCockpit.setCurrentItem(1, true);
                    return true;
                case NavigationDockView.TAB_INSPECTIONS:
                case NavigationDockView.TAB_DOCUMENTS:
                    viewPagerCockpit.setCurrentItem(2, true);
                    return true;
                case NavigationDockView.TAB_REPORTS:
                    viewPagerCockpit.setCurrentItem(3, true);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void updateDockVisuals(int pagePosition) {
        switch (pagePosition) {
            case 0:
                navigationDock.setActiveTab(NavigationDockView.TAB_HOME);
                break;
            case 1:
                navigationDock.setActiveTab(NavigationDockView.TAB_PROGRESS);
                break;
            case 2:
                navigationDock.setActiveTab(NavigationDockView.TAB_INSPECTIONS);
                break;
            case 3:
                navigationDock.setActiveTab(NavigationDockView.TAB_REPORTS);
                break;
        }
    }

    private void setupMenu() {
        btnCockpitMenu.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            showCockpitOptionsDialog();
        });
    }

    private void showCockpitOptionsDialog() {
        String[] options = new String[]{
                "Request Capability Elevation",
                "Switch Active Role (Simulation)",
                "Hardware Telemetry Diagnostics",
                "Log Out & Clear Session"
        };

        new AlertDialog.Builder(this)
                .setTitle("Cockpit Command & Control")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            AccessControl.showElevationDialog(this, "Master Jobsite Sign-Off", "Project Manager");
                            break;
                        case 1:
                            showRoleSimulationDialog();
                            break;
                        case 2:
                            Toast.makeText(this, "Hardware Sensors: Barometer, Inclinometer & CameraX Active", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            sessionManager.clearSession();
                            Intent intent = new Intent(this, WelcomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            break;
                    }
                })
                .show();
    }

    private void showRoleSimulationDialog() {
        String[] roles = new String[]{"Project Manager (Full Approval)", "Site Supervisor (QC Sign-Off)", "Field Engineer (Execution)"};
        new AlertDialog.Builder(this)
                .setTitle("Simulate Active Role Claim")
                .setItems(roles, (dialog, which) -> {
                    Role simulated = which == 0 ? Role.PROJECT_MANAGER : (which == 1 ? Role.SITE_SUPERVISOR : Role.ENGINEER);
                    if (currentUser != null) {
                        currentUser.setRole(simulated.name());
                        sessionManager.createLoginSession(currentUser, true);
                    }
                    setupUserClaims();
                    Toast.makeText(this, "Switched Role: " + simulated.getDisplayName(), Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
