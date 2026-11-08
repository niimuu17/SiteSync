package com.example.sitesync.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sitesync.data.AuthRepository;
import com.example.sitesync.data.SessionManager;
import com.example.sitesync.databinding.ActivityAuthBinding;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.google.android.material.tabs.TabLayout;

/**
 * Authentication Activity providing:
 * 1) Role awareness forwarded from Welcome screen
 * 2) Tabs for "Create ID" and "Log In"
 * 3) Multi-factor login methods: User ID + Password OR Phone Number + OTP
 * 4) Form validation and routing to role-specific dashboards
 */
public class AuthActivity extends AppCompatActivity {

    public static final String EXTRA_ROLE = "extra_role";

    private ActivityAuthBinding binding;
    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private Role selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();

        extractRole();
        initViews();
        setupListeners();
    }

    private void extractRole() {
        String roleExtra = getIntent().getStringExtra(EXTRA_ROLE);
        selectedRole = Role.fromString(roleExtra);
        binding.tvRoleSubtitle.setText("Role: " + selectedRole.getDisplayName());
    }

    private void initViews() {
        // Tab listener to toggle between Create ID and Log In containers
        binding.tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                hideError();
                hideKeyboard();
                if (tab.getPosition() == 0) {
                    binding.containerCreateId.setVisibility(View.VISIBLE);
                    binding.containerLogin.setVisibility(View.GONE);
                } else {
                    binding.containerCreateId.setVisibility(View.GONE);
                    binding.containerLogin.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Toggle between Method 1 (User ID + Password) and Method 2 (Phone + OTP)
        binding.rgLoginMethods.setOnCheckedChangeListener((group, checkedId) -> {
            hideError();
            hideKeyboard();
            if (checkedId == binding.rbMethodUserId.getId()) {
                binding.layoutMethodUserId.setVisibility(View.VISIBLE);
                binding.layoutMethodPhoneOtp.setVisibility(View.GONE);
            } else {
                binding.layoutMethodUserId.setVisibility(View.GONE);
                binding.layoutMethodPhoneOtp.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupListeners() {
        // Back button to change role
        binding.btnBackToRoles.setOnClickListener(v -> finish());

        // 1) Create ID submission
        binding.btnSubmitCreateId.setOnClickListener(v -> handleCreateId());

        // 2) Log In via Method 1: User ID + Password
        binding.btnLoginWithPassword.setOnClickListener(v -> handleLoginWithPassword());

        // 3) Log In via Method 2: Request OTP
        binding.btnRequestOtp.setOnClickListener(v -> handleRequestOtp());

        // 4) Log In via Method 2: Verify OTP
        binding.btnLoginWithOtp.setOnClickListener(v -> handleLoginWithOtp());
    }

    // =========================================================================
    // 1. CREATE ID / REGISTRATION FLOW
    // =========================================================================
    private void handleCreateId() {
        hideKeyboard();
        hideError();

        String name = getText(binding.etCreateName);
        String country = getText(binding.etCreateCountry);
        String email = getText(binding.etCreateEmail);
        String phone = getText(binding.etCreatePhone);
        String userId = getText(binding.etCreateUserId);
        String password = getText(binding.etCreatePassword);

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.tilCreateName.setError("Full name is required");
            return;
        } else {
            binding.tilCreateName.setError(null);
        }

        if (TextUtils.isEmpty(country)) {
            binding.tilCreateCountry.setError("Country / region is required");
            return;
        } else {
            binding.tilCreateCountry.setError(null);
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilCreateEmail.setError("Enter a valid email address");
            return;
        } else {
            binding.tilCreateEmail.setError(null);
        }

        if (TextUtils.isEmpty(phone) || phone.length() < 7) {
            binding.tilCreatePhone.setError("Enter a valid phone number");
            return;
        } else {
            binding.tilCreatePhone.setError(null);
        }

        if (TextUtils.isEmpty(userId) || userId.length() < 3) {
            binding.tilCreateUserId.setError("User ID must be at least 3 characters");
            return;
        } else {
            binding.tilCreateUserId.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilCreatePassword.setError("Password must be at least 6 characters");
            return;
        } else {
            binding.tilCreatePassword.setError(null);
        }

        setLoading(true);
        authRepository.registerUser(name, country, email, phone, userId, password, selectedRole, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user, true);
                Toast.makeText(AuthActivity.this, "Welcome " + user.getFullName() + "!", Toast.LENGTH_SHORT).show();
                AuthRouter.routeToDashboard(AuthActivity.this, selectedRole, user);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    // =========================================================================
    // 2. LOG IN VIA METHOD 1: USER ID + PASSWORD
    // =========================================================================
    private void handleLoginWithPassword() {
        hideKeyboard();
        hideError();

        String userId = getText(binding.etLoginUserId);
        String password = getText(binding.etLoginPassword);

        if (TextUtils.isEmpty(userId)) {
            binding.tilLoginUserId.setError("Please enter your User ID");
            return;
        } else {
            binding.tilLoginUserId.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.tilLoginPassword.setError("Password must be at least 6 characters");
            return;
        } else {
            binding.tilLoginPassword.setError(null);
        }

        setLoading(true);
        authRepository.loginWithCredentials(userId, password, selectedRole, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user, true);
                Toast.makeText(AuthActivity.this, "Signed in as " + selectedRole.getDisplayName(), Toast.LENGTH_SHORT).show();
                AuthRouter.routeToDashboard(AuthActivity.this, selectedRole, user);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    // =========================================================================
    // 3. LOG IN VIA METHOD 2: PHONE + OTP
    // =========================================================================
    private void handleRequestOtp() {
        hideKeyboard();
        hideError();

        String phone = getText(binding.etLoginPhone);
        if (TextUtils.isEmpty(phone) || phone.length() < 7) {
            binding.tilLoginPhone.setError("Enter a valid phone number");
            return;
        } else {
            binding.tilLoginPhone.setError(null);
        }

        setLoading(true);
        authRepository.sendOtp(phone, new AuthRepository.OtpCallback() {
            @Override
            public void onOtpSent(String generatedOtp, String message) {
                setLoading(false);
                binding.etLoginOtp.setText(generatedOtp); // Auto-fill mock code for test convenience
                Toast.makeText(AuthActivity.this, "OTP sent: " + generatedOtp, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    private void handleLoginWithOtp() {
        hideKeyboard();
        hideError();

        String phone = getText(binding.etLoginPhone);
        String otp = getText(binding.etLoginOtp);

        if (TextUtils.isEmpty(phone) || phone.length() < 7) {
            binding.tilLoginPhone.setError("Enter a valid phone number");
            return;
        }

        if (TextUtils.isEmpty(otp) || otp.length() != 6) {
            binding.tilLoginOtp.setError("Please enter the 6-digit OTP");
            return;
        } else {
            binding.tilLoginOtp.setError(null);
        }

        setLoading(true);
        authRepository.verifyOtpAndLogin(phone, otp, selectedRole, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                sessionManager.createLoginSession(user, true);
                Toast.makeText(AuthActivity.this, "OTP Verified. Welcome!", Toast.LENGTH_SHORT).show();
                AuthRouter.routeToDashboard(AuthActivity.this, selectedRole, user);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                showError(errorMessage);
            }
        });
    }

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================
    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void setLoading(boolean isLoading) {
        binding.progressAuth.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSubmitCreateId.setEnabled(!isLoading);
        binding.btnLoginWithPassword.setEnabled(!isLoading);
        binding.btnLoginWithOtp.setEnabled(!isLoading);
        binding.btnRequestOtp.setEnabled(!isLoading);
    }

    private void showError(String message) {
        binding.tvAuthError.setText(message);
        binding.tvAuthError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvAuthError.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }
}
