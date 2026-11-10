package com.example.sitesync.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.R;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.data.local.AppDatabase;
import com.example.sitesync.data.local.entities.WorkerJoinRequestEntity;
import com.example.sitesync.databinding.ActivitySignupBinding;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.example.sitesync.utils.HapticFeedbackUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Screen 2: Sign-Up Screen
 * Adheres strictly to the Oxide Blue & Warm Ash dual-tone identity.
 * Validates account information, registers with Firebase Auth, and creates profile in Firestore.
 */
public class SignUpActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private SessionManager sessionManager;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFooterSpannable();
        setupListeners();
    }

    private void setupFooterSpannable() {
        String fullText = "Already have an account? Log In";
        SpannableString ss = new SpannableString(fullText);
        int start = fullText.indexOf("Log In");
        if (start != -1) {
            ss.setSpan(new StyleSpan(Typeface.BOLD), start, start + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new UnderlineSpan(), start, start + 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.tvFooterLogIn.setText(ss);
    }

    private void setupListeners() {
        // Password Visibility Toggle
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

        // Confirm Password Visibility Toggle
        binding.ivToggleConfirmPassword.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                binding.etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye_off);
            } else {
                binding.etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye);
            }
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.getText().length());
        });

        // Role Selector Toggle
        binding.rgAccountRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRoleWorker) {
                binding.layoutWorkerFields.setVisibility(View.VISIBLE);
            } else {
                binding.layoutWorkerFields.setVisibility(View.GONE);
            }
        });

        // Sign Up button
        binding.btnSignUp.setOnClickListener(v -> {
            HapticFeedbackUtil.heavyClick(this, v);
            attemptSignUp();
        });

        // Footer: Navigate to Login
        binding.tvFooterLogIn.setOnClickListener(v -> {
            HapticFeedbackUtil.lightClick(this, v);
            finish();
        });
    }

    private void attemptSignUp() {
        hideKeyboard();
        hideErrorBanner();

        String fullName = binding.etFullName.getText() != null ? binding.etFullName.getText().toString().trim() : "";
        String usernameOrEmail = binding.etUsernameOrEmail.getText() != null ? binding.etUsernameOrEmail.getText().toString().trim() : "";
        String mobile = binding.etMobileNumber.getText() != null ? binding.etMobileNumber.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString().trim() : "";

        // Validations
        if (fullName.length() < 3) {
            showErrorBanner("Full Name must be at least 3 characters.");
            return;
        }

        if (TextUtils.isEmpty(usernameOrEmail)) {
            showErrorBanner("Please enter a Username or Gmail address.");
            return;
        }

        if (mobile.length() < 7) {
            showErrorBanner("Please enter a valid mobile number.");
            return;
        }

        if (password.length() < 6) {
            showErrorBanner("Password must be at least 6 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showErrorBanner("Passwords do not match.");
            return;
        }

        boolean isWorker = binding.rbRoleWorker.isChecked();
        String selectedRole = isWorker ? Role.WORKER.name() : Role.PROJECT_MANAGER.name();
        String workerProjectCode = binding.etWorkerProjectCode.getText() != null ?
                binding.etWorkerProjectCode.getText().toString().trim().toUpperCase() : "";
        String workerTrade = binding.etWorkerTrade.getText() != null ?
                binding.etWorkerTrade.getText().toString().trim() : "";

        if (isWorker && TextUtils.isEmpty(workerProjectCode)) {
            showErrorBanner("Please enter the Project Code to join (e.g. PRJ-DHAKA-101).");
            return;
        }

        setLoading(true);

        String email = usernameOrEmail.contains("@")
                ? usernameOrEmail
                : usernameOrEmail.toLowerCase().replaceAll("[^a-z0-9_]", "") + "@sitesync.io";
        String username = usernameOrEmail.contains("@")
                ? usernameOrEmail.split("@")[0]
                : usernameOrEmail;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    String uid = firebaseUser != null ? firebaseUser.getUid() : ("user_" + System.currentTimeMillis());

                    // Prepare Firestore user profile
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("uid", uid);
                    profile.put("fullName", fullName);
                    profile.put("username", username);
                    profile.put("email", email);
                    profile.put("mobile", mobile);
                    profile.put("role", selectedRole);
                    profile.put("projectCode", workerProjectCode);
                    profile.put("trade", workerTrade);
                    profile.put("createdAt", FieldValue.serverTimestamp());

                    // If Worker, record Join Request in Room DB & Firestore
                    if (isWorker) {
                        WorkerJoinRequestEntity joinRequest = new WorkerJoinRequestEntity(
                                UUID.randomUUID().toString(), uid, fullName, mobile,
                                !TextUtils.isEmpty(workerTrade) ? workerTrade : "General Site Worker",
                                workerProjectCode, "PENDING", System.currentTimeMillis()
                        );
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getInstance(SignUpActivity.this).workerJoinRequestDao().insert(joinRequest);
                        });
                        sessionManager.setActiveProject(1L, workerProjectCode, "Site Project: " + workerProjectCode);
                    }

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(profile)
                            .addOnCompleteListener(task -> {
                                setLoading(false);
                                User user = new User(username, fullName, selectedRole, uid, email, mobile, "BD");
                                sessionManager.createLoginSession(user, true);
                                Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                navigateToMainDashboard();
                            });
                })
                .addOnFailureListener(e -> {
                    // Fallback to local user session if Firebase Auth is in offline or mock mode
                    setLoading(false);
                    String uid = "user_" + System.currentTimeMillis();
                    if (isWorker) {
                        WorkerJoinRequestEntity joinRequest = new WorkerJoinRequestEntity(
                                UUID.randomUUID().toString(), uid, fullName, mobile,
                                !TextUtils.isEmpty(workerTrade) ? workerTrade : "General Site Worker",
                                workerProjectCode, "PENDING", System.currentTimeMillis()
                        );
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getInstance(SignUpActivity.this).workerJoinRequestDao().insert(joinRequest);
                        });
                        sessionManager.setActiveProject(1L, workerProjectCode, "Site Project: " + workerProjectCode);
                    }
                    User user = new User(username, fullName, selectedRole, uid, email, mobile, "BD");
                    sessionManager.createLoginSession(user, true);
                    Toast.makeText(SignUpActivity.this, "Account registered: " + fullName, Toast.LENGTH_SHORT).show();
                    navigateToMainDashboard();
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBarSignUp.setVisibility(View.VISIBLE);
            binding.btnSignUp.setText("");
            binding.btnSignUp.setEnabled(false);
        } else {
            binding.progressBarSignUp.setVisibility(View.GONE);
            binding.btnSignUp.setText("Sign Up");
            binding.btnSignUp.setEnabled(true);
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
        finishAffinity();
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
