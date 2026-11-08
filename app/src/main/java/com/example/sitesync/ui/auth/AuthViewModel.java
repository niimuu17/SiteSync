package com.example.sitesync.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.sitesync.data.AuthRepository;
import com.example.sitesync.models.Role;
import com.example.sitesync.models.User;

/**
 * Enterprise AuthViewModel managing authentication states and 2FA challenge responses.
 */
public class AuthViewModel extends AndroidViewModel {

    public static class AuthState {
        public final boolean isLoading;
        public final User user;
        public final String errorMessage;
        public final boolean requires2Fa;
        public final String destinationPhone;

        public AuthState(boolean isLoading, User user, String errorMessage, boolean requires2Fa, String destinationPhone) {
            this.isLoading = isLoading;
            this.user = user;
            this.errorMessage = errorMessage;
            this.requires2Fa = requires2Fa;
            this.destinationPhone = destinationPhone;
        }

        public static AuthState loading() {
            return new AuthState(true, null, null, false, null);
        }

        public static AuthState success(User user) {
            return new AuthState(false, user, null, false, null);
        }

        public static AuthState error(String message) {
            return new AuthState(false, null, message, false, null);
        }

        public static AuthState mfaRequired(User partialUser, String phone) {
            return new AuthState(false, partialUser, null, true, phone);
        }
    }

    private final AuthRepository authRepository;
    private final MutableLiveData<AuthState> authStateLiveData = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<AuthState> getAuthState() {
        return authStateLiveData;
    }

    public void login(String usernameOrEmail, String password) {
        authStateLiveData.setValue(AuthState.loading());
        authRepository.signIn(usernameOrEmail, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authStateLiveData.postValue(AuthState.success(user));
            }

            @Override
            public void onError(String errorMessage) {
                authStateLiveData.postValue(AuthState.error(errorMessage));
            }
        }, (partialUser, destinationPhone) -> {
            authStateLiveData.postValue(AuthState.mfaRequired(partialUser, destinationPhone));
        });
    }

    public void register(String name, String country, String email, String phone,
                         String username, String password, Role role) {
        authStateLiveData.setValue(AuthState.loading());
        authRepository.signUp(email, password, name, username, phone, country, role, true, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authStateLiveData.postValue(AuthState.success(user));
            }

            @Override
            public void onError(String errorMessage) {
                authStateLiveData.postValue(AuthState.error(errorMessage));
            }
        });
    }
}
