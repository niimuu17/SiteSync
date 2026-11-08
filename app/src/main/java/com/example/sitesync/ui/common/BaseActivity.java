package com.example.sitesync.ui.common;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sitesync.R;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.android.material.snackbar.Snackbar;

/**
 * BaseActivity enforcing uniform high-contrast dark industrial telemetry styling:
 * #111317 Canvas, #1A1D24 Surfaces, #F59E0B Safety Accents, #2E3442 1dp Borders.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyIndustrialTheme();
    }

    protected void applyIndustrialTheme() {
        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.bg_canvas));
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.bg_canvas));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ensure status bar icons are light on dark background
                View decor = window.getDecorView();
                decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    public void showIndustrialMessage(View anchorView, String message) {
        if (anchorView == null || message == null) return;
        Snackbar snackbar = Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.bg_surface_card));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.accent_safety_yellow));
        snackbar.show();
    }

    public void triggerHaptic(View view) {
        HapticFeedbackUtil.heavyClick(this, view);
    }
}
