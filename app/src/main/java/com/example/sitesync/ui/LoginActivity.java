package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.R;
import com.example.sitesync.data.FirebaseAuthManager;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityLoginBinding;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.example.sitesync.ui.auth.TwoFactorBottomSheetDialog;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Screen 1: Login Screen
 * Adheres strictly to the Oxide Blue & Warm Ash dual-tone identity.
 * Authenticates with Firebase Auth (email/password or username resolution) and intercepts MFA.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private FirebaseAuthManager firebaseAuthManager;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        firebaseAuthManager = FirebaseAuthManager.getInstance(this);

        // Check if user session already exists
        if (sessionManager.isLoggedIn()) {
            navigateToMainDashboard();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFooterSpannable();
        setupListeners();
    }

    private void setupFooterSpannable() {
        String fullText = "Don't have an account? Sign Up";
        SpannableString ss = new SpannableString(fullText);
        int start = fullText.indexOf("Sign Up");
        if (start != -1) {
            ss.setSpan(new StyleSpan(Typeface.BOLD), start, start + 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new UnderlineSpan(), start, start + 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.tvFooterSignUp.setText(ss);
    }

    private void setupListeners() {
        // Password Visibility Toggle Eye
        binding.ivTogglePassword.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_eye_off);
            } else {
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_eye);
            }
            binding.etPassword.setSelection(binding.etPassword.getText().length());
        });

        // Trigger Log In on IME Done
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Primary Log In button
        binding.btnLogIn.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            attemptLogin();
        });

        // Demo Autofill
        binding.btnFillDemo.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            binding.etUsername.setText("elena@sitesync.com");
            binding.etPassword.setText("sitesync123");
            hideErrorBanner();
            binding.btnLogIn.requestFocus();
        });

        // Footer: Navigate to Sign-Up
        binding.tvFooterSignUp.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        hideKeyboard();
        hideErrorBanner();

        String input = binding.etUsername.getText() != null ? binding.etUsername.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(input)) {
            showErrorBanner("Please enter your Username or Gmail address.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showErrorBanner("Please enter your password.");
            return;
        }

        if (password.length() < 6) {
            showErrorBanner("Password must be at least 6 characters.");
            return;
        }

        setLoading(true);

        // Check if input is a Gmail/email address or username
        if (input.contains("@")) {
            executeFirebaseSignIn(input, password);
        } else {
            resolveUsernameAndSignIn(input, password);
        }
    }

    private void resolveUsernameAndSignIn(String username, String password) {
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String email = queryDocumentSnapshots.getDocuments().get(0).getString("email");
                        if (!TextUtils.isEmpty(email)) {
                            executeFirebaseSignIn(email, password);
                            return;
                        }
                    }
                    // If not found in Firestore or network unavailable, test demo users
                    fallbackDemoCheck(username, password);
                })
                .addOnFailureListener(e -> {
                    // Fallback to demo check on offline or firestore error
                    fallbackDemoCheck(username, password);
                });
    }

    private void executeFirebaseSignIn(String email, String password) {
        firebaseAuthManager.signInWithEmailAndPassword(this, email, password, new FirebaseAuthManager.AuthMfaCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user, true);
                Toast.makeText(LoginActivity.this, "Signed in as " + user.getFullName(), Toast.LENGTH_SHORT).show();
                navigateToMainDashboard();
            }

            @Override
            public void onMfaRequired(com.google.firebase.auth.MultiFactorResolver resolver,
                                      String verificationIdHint, String destinationPhoneHint) {
                setLoading(false);
                User pendingUser = new User("user_mfa", "Field Engineer", Role.ENGINEER.name(), "TOKEN", email, destinationPhoneHint, "US");
                TwoFactorBottomSheetDialog dialog = TwoFactorBottomSheetDialog.newInstance(
                        destinationPhoneHint, verificationIdHint, pendingUser, resolver
                );
                dialog.setOnVerificationCompleteListener(user -> {
                    sessionManager.createLoginSession(user, true);
                    navigateToMainDashboard();
                });
                dialog.show(getSupportFragmentManager(), "TwoFactorBottomSheetDialog");
            }

            @Override
            public void onError(String errorMessage) {
                // If standard Firebase sign-in fails, check for demo credentials fallback
                if (!fallbackDemoCheck(email, password)) {
                    setLoading(false);
                    showErrorBanner(errorMessage);
                }
            }
        });
    }

    private boolean fallbackDemoCheck(String credential, String password) {
        if ("sitesync123".equals(password) || "password".equals(password) || "123456".equals(password)) {
            setLoading(false);
            User demoUser;
            if (credential.contains("worker")) {
                demoUser = new User("worker_rafiq", "Md. Rafiqul Islam", Role.WORKER.name(), "DEMO-TOKEN", "rafiq@sitesync.io", "+880 1712-345678", "BD");
            } else if (credential.contains("manager")) {
                demoUser = new User("manager_1", "Marcus Vance", Role.PROJECT_MANAGER.name(), "DEMO-TOKEN", "marcus@sitesync.com", "+1-555-0101", "US");
            } else if (credential.contains("supervisor")) {
                demoUser = new User("super_1", "Alex Chen", Role.SITE_SUPERVISOR.name(), "DEMO-TOKEN", "alex@sitesync.com", "+1-555-0102", "US");
            } else {
                demoUser = new User("eng_1", "Elena Rostova", Role.ENGINEER.name(), "DEMO-TOKEN", "elena@sitesync.com", "+1-555-0199", "US");
            }
            sessionManager.createLoginSession(demoUser, true);
            Toast.makeText(this, "Authenticated: " + demoUser.getFullName() + " (" + demoUser.getRole() + ")", Toast.LENGTH_SHORT).show();
            navigateToMainDashboard();
            return true;
        }
        setLoading(false);
        showErrorBanner("Invalid credentials. Please verify your username and password.");
        return false;
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnLogIn.setText("");
            binding.btnLogIn.setEnabled(false);
            binding.etUsername.setEnabled(false);
            binding.etPassword.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogIn.setText("Log In");
            binding.btnLogIn.setEnabled(true);
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

    private void navigateToMainDashboard() {
        Intent intent;
        if (sessionManager.isWorkerRole()) {
            intent = new Intent(this, WorkerDashboardActivity.class);
        } else {
            intent = new Intent(this, ProjectHubActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
}
