package com.example.sitesync.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.R;
import com.example.sitesync.data.AuthRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityLoginBinding;
import com.example.sitesync.models.User;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();

        // Check if user is already authenticated
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        setupListeners();
    }

    private void initViews() {
        // Restore remembered username if present
        if (sessionManager.isRememberMeEnabled()) {
            String savedUsername = sessionManager.getSavedUsername();
            if (!savedUsername.isEmpty()) {
                binding.etUsername.setText(savedUsername);
                binding.cbRememberMe.setChecked(true);
                binding.etPassword.requestFocus();
            }
        }
    }

    private void setupListeners() {
        // Clear errors upon typing
        binding.etUsername.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilUsername.setError(null);
                hideErrorBanner();
            }
        });

        binding.etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilPassword.setError(null);
                hideErrorBanner();
            }
        });

        // Trigger sign-in on keyboard "Done" action
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Sign In Button
        binding.btnSignIn.setOnClickListener(v -> attemptLogin());

        // Forgot Password
        binding.btnForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Quick demo role fill chips
        binding.chipManager.setOnClickListener(v -> fillDemoCredentials("manager", "sitesync123"));
        binding.chipEngineer.setOnClickListener(v -> fillDemoCredentials("engineer", "sitesync123"));
        binding.chipSupervisor.setOnClickListener(v -> fillDemoCredentials("supervisor", "sitesync123"));
    }

    private void fillDemoCredentials(String username, String password) {
        binding.etUsername.setText(username);
        binding.etPassword.setText(password);
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);
        hideErrorBanner();
        binding.btnSignIn.requestFocus();
    }

    private void attemptLogin() {
        hideKeyboard();
        hideErrorBanner();

        String username = binding.etUsername.getText() != null ? binding.etUsername.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        boolean hasError = false;

        if (username.isEmpty()) {
            binding.tilUsername.setError(getString(R.string.error_empty_username));
            hasError = true;
        }

        if (password.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            hasError = true;
        } else if (password.length() < 6) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        setLoading(true);

        authRepository.login(username, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                boolean rememberMe = binding.cbRememberMe.isChecked();
                sessionManager.createLoginSession(user, rememberMe);

                Toast.makeText(LoginActivity.this, "Authenticated as " + user.getRole(), Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showErrorBanner(errorMessage);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSignIn.setText("");
            binding.btnSignIn.setEnabled(false);
            binding.etUsername.setEnabled(false);
            binding.etPassword.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSignIn.setText(R.string.sign_in_button);
            binding.btnSignIn.setEnabled(true);
            binding.etUsername.setEnabled(true);
            binding.etPassword.setEnabled(true);
        }
    }

    private void showErrorBanner(String message) {
        binding.tvErrorMessage.setText(message);
        binding.tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideErrorBanner() {
        binding.tvErrorMessage.setVisibility(View.GONE);
    }

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Credential Recovery")
                .setMessage(getString(R.string.forgot_password_info))
                .setPositiveButton("Close", null)
                .show();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
