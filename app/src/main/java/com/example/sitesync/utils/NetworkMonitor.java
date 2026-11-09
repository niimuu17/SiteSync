package com.example.sitesync.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Resilient Network Connectivity Monitor for field conditions.
 * Broadcasts connection state changes to automatically toggle offline queues.
 */
public class NetworkMonitor {

    private static volatile NetworkMonitor instance;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isOnlineLiveData = new MutableLiveData<>(false);

    private NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialState();
        registerCallback();
    }

    public static NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) {
                    instance = new NetworkMonitor(context);
                }
            }
        }
        return instance;
    }

    private void checkInitialState() {
        if (connectivityManager == null) {
            isOnlineLiveData.postValue(false);
            return;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            boolean connected = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            isOnlineLiveData.postValue(connected);
        } else {
            isOnlineLiveData.postValue(false);
        }
    }

    private void registerCallback() {
        if (connectivityManager == null) return;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isOnlineLiveData.postValue(true);
            }

            @Override
            public void onLost(@NonNull Network network) {
                isOnlineLiveData.postValue(false);
            }
        });
    }

    public LiveData<Boolean> isOnline() {
        return isOnlineLiveData;
    }

    public boolean isCurrentlyOnline() {
        if (connectivityManager != null) {
            try {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (capabilities != null &&
                            (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                             capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Boolean value = isOnlineLiveData.getValue();
        return value != null && value;
    }
}
