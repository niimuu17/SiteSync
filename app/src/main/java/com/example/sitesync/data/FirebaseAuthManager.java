package com.example.sitesync.data;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorAssertion;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.auth.PhoneMultiFactorInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

/**
 * Enterprise Authentication Manager handling Firebase Auth with Multi-Factor Authentication (MFA),
 * SMS Challenge-Response resolvers, and Custom Claims RBAC evaluation.
 */
public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuthManager";
    private static volatile FirebaseAuthManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthMfaCallback {
        void onSuccess(User user);
        void onMfaRequired(MultiFactorResolver resolver, String verificationIdHint, String destinationPhoneHint);
        void onError(String errorMessage);
    }

    public interface MfaVerificationCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    private FirebaseAuthManager(Context context) {
        FirebaseSyncManager syncManager = FirebaseSyncManager.getInstance(context);
        this.auth = syncManager.getAuth() != null ? syncManager.getAuth() : FirebaseAuth.getInstance();
        this.firestore = syncManager.getFirestore() != null ? syncManager.getFirestore() : FirebaseFirestore.getInstance();
    }

    public static FirebaseAuthManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FirebaseAuthManager.class) {
                if (instance == null) {
                    instance = new FirebaseAuthManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Primary Email/Password sign-in with 2-Step Verification (MFA) challenge interception.
     */
    public void signInWithEmailAndPassword(Activity activity, String email, String password, AuthMfaCallback callback) {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            callback.onError("Please enter both email and password.");
            return;
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        resolveRoleAndBuildUser(firebaseUser, callback::onSuccess, callback::onError);
                    } else {
                        callback.onError("Failed to obtain authenticated session.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthMultiFactorException) {
                        // MFA Challenge Intercepted
                        FirebaseAuthMultiFactorException mfaException = (FirebaseAuthMultiFactorException) e;
                        MultiFactorResolver resolver = mfaException.getResolver();
                        handleMfaChallenge(activity, resolver, callback);
                    } else {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Authentication failed.");
                    }
                });
    }

    private void handleMfaChallenge(Activity activity, MultiFactorResolver resolver, AuthMfaCallback callback) {
        if (resolver.getHints().isEmpty()) {
            callback.onError("No enrolled second factor found for this account.");
            return;
        }

        PhoneMultiFactorInfo phoneHint = (PhoneMultiFactorInfo) resolver.getHints().get(0);
        String destinationPhone = phoneHint.getPhoneNumber();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder()
                .setPhoneNumber(destinationPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-retrieval completed
                        resolveSignInWithCredential(resolver, credential, new MfaVerificationCallback() {
                            @Override
                            public void onSuccess(User user) {
                                callback.onSuccess(user);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                callback.onError(errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        callback.onError("SMS verification dispatch failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mainHandler.post(() -> callback.onMfaRequired(resolver, verificationId, destinationPhone));
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Resolves the second factor verification challenge with the user-entered 6-digit SMS code.
     */
    public void resolveSignInWithSmsCode(MultiFactorResolver resolver, String verificationId,
                                         String smsCode, MfaVerificationCallback callback) {
        if (smsCode == null || smsCode.trim().length() < 6) {
            callback.onError("Please enter a valid 6-digit verification code.");
            return;
        }

        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode.trim());
            resolveSignInWithCredential(resolver, credential, callback);
        } catch (Exception e) {
            callback.onError("Failed to create verification assertion: " + e.getMessage());
        }
    }

    private void resolveSignInWithCredential(MultiFactorResolver resolver, PhoneAuthCredential credential,
                                             MfaVerificationCallback callback) {
        MultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
        resolver.resolveSignIn(assertion)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        resolveRoleAndBuildUser(firebaseUser, callback::onSuccess, callback::onError);
                    } else {
                        callback.onError("Unable to complete MFA session.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage() != null ? e.getMessage() : "MFA resolution failed."));
    }

    /**
     * Fetches validated Custom Claims and Firestore Role profile.
     */
    public void resolveRoleAndBuildUser(FirebaseUser fbUser, java.util.function.Consumer<User> onSuccess,
                                        java.util.function.Consumer<String> onError) {
        String uid = fbUser.getUid();
        String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";

        // First inspect Firebase ID Token Custom Claims
        fbUser.getIdToken(false).addOnSuccessListener(getTokensResult -> {
            Object roleClaim = getTokensResult.getClaims().get("role");
            String assignedRole = roleClaim != null ? roleClaim.toString() : null;

            // Also read Firestore /users/{uid} profile
            firestore.collection("users").document(uid).get()
                    .addOnSuccessListener(snapshot -> {
                        User user = new User();
                        user.setUid(uid);
                        user.setEmail(email);

                        if (snapshot.exists() && snapshot.getData() != null) {
                            user = User.fromFirestoreMap(uid, snapshot.getData());
                        } else {
                            user.setUsername(email.contains("@") ? email.split("@")[0] : "engineer");
                            user.setFullName("Field Engineer");
                            user.setRole(assignedRole != null ? assignedRole : Role.ENGINEER.name());
                        }

                        if (assignedRole != null) {
                            user.setRole(assignedRole);
                        }

                        User finalUser = user;
                        mainHandler.post(() -> onSuccess.accept(finalUser));
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Offline user profile fallback: " + e.getMessage());
                        User fallback = new User();
                        fallback.setUid(uid);
                        fallback.setEmail(email);
                        fallback.setUsername(email.contains("@") ? email.split("@")[0] : "engineer");
                        fallback.setFullName("Field Engineer");
                        fallback.setRole(assignedRole != null ? assignedRole : Role.ENGINEER.name());
                        mainHandler.post(() -> onSuccess.accept(fallback));
                    });
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Token claims read failure, fallback to firestore: " + e.getMessage());
            firestore.collection("users").document(uid).get()
                    .addOnSuccessListener(snapshot -> {
                        User user = User.fromFirestoreMap(uid, snapshot.getData());
                        mainHandler.post(() -> onSuccess.accept(user));
                    })
                    .addOnFailureListener(err -> onError.accept(err.getMessage()));
        });
    }

    public FirebaseAuth getAuth() {
        return auth;
    }
}
