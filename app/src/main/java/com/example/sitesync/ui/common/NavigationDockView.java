package com.example.sitesync.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.utils.HapticFeedbackUtil;

/**
 * Floating Navigation Dock View
 * Strict industrial dark pill dock (#1A1D24 with 1dp border #2E3442).
 * Fast one-tap switching between the 5 core engineering modules.
 */
public class NavigationDockView extends FrameLayout {

    public static final int TAB_HOME = 0;
    public static final int TAB_PROGRESS = 1;
    public static final int TAB_INSPECTIONS = 2;
    public static final int TAB_DOCUMENTS = 3;
    public static final int TAB_REPORTS = 4;

    private int activeTab = TAB_HOME;
    private final ImageView[] tabIcons = new ImageView[5];

    public NavigationDockView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public NavigationDockView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public NavigationDockView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setBackgroundResource(R.drawable.bg_floating_dock);
        int padH = dpToPx(16);
        int padV = dpToPx(8);
        setPadding(padH, padV, padH, padV);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams rowLp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowLp);

        int[] drawables = new int[]{
                R.drawable.ic_home_dock,
                R.drawable.ic_chart_toggle,
                R.drawable.ic_inspection,
                R.drawable.ic_document,
                R.drawable.ic_comment
        };

        for (int i = 0; i < 5; i++) {
            final int index = i;
            FrameLayout itemContainer = new FrameLayout(context);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, dpToPx(48), 1.0f);
            itemContainer.setLayoutParams(itemLp);

            ImageView iv = new ImageView(context);
            iv.setImageResource(drawables[i]);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24), Gravity.CENTER);
            iv.setLayoutParams(ivLp);
            tabIcons[i] = iv;

            itemContainer.addView(iv);
            itemContainer.setOnClickListener(v -> onTabClicked(index));

            row.addView(itemContainer);
        }

        addView(row);
        updateTabVisuals();
    }

    public void setActiveTab(int tabIndex) {
        this.activeTab = tabIndex;
        updateTabVisuals();
    }

    private void updateTabVisuals() {
        for (int i = 0; i < tabIcons.length; i++) {
            if (tabIcons[i] == null) continue;
            if (i == activeTab) {
                tabIcons[i].setColorFilter(ContextCompat.getColor(getContext(), R.color.accent_safety_yellow));
                tabIcons[i].setScaleX(1.15f);
                tabIcons[i].setScaleY(1.15f);
            } else {
                tabIcons[i].setColorFilter(ContextCompat.getColor(getContext(), R.color.text_secondary));
                tabIcons[i].setScaleX(1.0f);
                tabIcons[i].setScaleY(1.0f);
            }
        }
    }

    private void onTabClicked(int tabIndex) {
        if (tabIndex == activeTab) return;
        HapticFeedbackUtil.heavyClick(getContext(), this);
        setActiveTab(tabIndex);

        Context ctx = getContext();
        if (!(ctx instanceof Activity)) return;
        Activity activity = (Activity) ctx;

        Intent intent = null;
        switch (tabIndex) {
            case TAB_HOME:
                intent = new Intent(activity, com.example.sitesync.ui.EngineerDashboardActivity.class);
                break;
            case TAB_PROGRESS:
                intent = new Intent(activity, com.example.sitesync.ui.ProgressMonitoringActivity.class);
                break;
            case TAB_INSPECTIONS:
                intent = new Intent(activity, com.example.sitesync.ui.InspectionListActivity.class);
                break;
            case TAB_DOCUMENTS:
                intent = new Intent(activity, com.example.sitesync.ui.DocumentManagementActivity.class);
                break;
            case TAB_REPORTS:
                intent = new Intent(activity, com.example.sitesync.ui.DailyReportListActivity.class);
                break;
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
