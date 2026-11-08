package com.example.sitesync.data.remote;

import android.app.Activity;
import android.content.Context;

import com.example.sitesync.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.MultiFactorResolver;

/**
 * Enterprise Remote Authentication Manager alias located in data.remote package.
 */
public class FirebaseAuthManager {

    private final com.example.sitesync.data.FirebaseAuthManager delegate;

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
        this.delegate = com.example.sitesync.data.FirebaseAuthManager.getInstance(context);
    }

    public static FirebaseAuthManager getInstance(Context context) {
        return new FirebaseAuthManager(context);
    }

    public void signInWithEmailAndPassword(Activity activity, String email, String password, AuthMfaCallback callback) {
        delegate.signInWithEmailAndPassword(activity, email, password, new com.example.sitesync.data.FirebaseAuthManager.AuthMfaCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onMfaRequired(MultiFactorResolver resolver, String verificationIdHint, String destinationPhoneHint) {
                callback.onMfaRequired(resolver, verificationIdHint, destinationPhoneHint);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void resolveSignInWithSmsCode(MultiFactorResolver resolver, String verificationId,
                                         String smsCode, MfaVerificationCallback callback) {
        delegate.resolveSignInWithSmsCode(resolver, verificationId, smsCode, new com.example.sitesync.data.FirebaseAuthManager.MfaVerificationCallback() {
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

    public FirebaseAuth getAuth() {
        return delegate.getAuth();
    }
}
