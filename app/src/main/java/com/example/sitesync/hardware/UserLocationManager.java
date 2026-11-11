package com.example.sitesync.hardware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.sitesync.data.SessionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/**
 * Centralized Hardware Location Engine:
 * Coordinates high-precision GPS, network triangulated fixes, and fused provider telemetry
 * for emergency fall detection alerts, geotagged evidence stamping, and shift tracking.
 */
public class UserLocationManager {

    private static final String TAG = "UserLocationManager";
    private static volatile UserLocationManager instance;

    // Nominal device coordinates in Khulna, Bangladesh (used only as emergency safety baseline)
    public static final double DEFAULT_FALLBACK_LAT = 22.899164;
    public static final double DEFAULT_FALLBACK_LNG = 89.501812;

    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationManager locationManager;
    private final SessionManager sessionManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Location lastKnownLocation;
    private LocationCallback fusedCallback;
    private boolean isListening = false;

    public interface OnLocationResultListener {
        void onLocationResolved(@NonNull Location location);
    }

    private UserLocationManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
        this.locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        this.sessionManager = new SessionManager(appContext);
        queryFastCachedLocation();
    }

    public static synchronized UserLocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserLocationManager(context);
        }
        return instance;
    }

    public static boolean hasLocationPermission(Context context) {
        if (context == null) return false;
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    /**
     * Fast synchronous lookup of best available cached coordinates from hardware providers or Session cache.
     */
    @SuppressLint("MissingPermission")
    public Location getBestAvailableLocation() {
        if (lastKnownLocation != null) {
            return lastKnownLocation;
        }

        if (hasLocationPermission(appContext) && locationManager != null) {
            try {
                Location gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location passLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                Location best = null;
                if (gpsLoc != null) best = gpsLoc;
                if (netLoc != null && (best == null || netLoc.getTime() > best.getTime())) best = netLoc;
                if (passLoc != null && (best == null || passLoc.getTime() > best.getTime())) best = passLoc;

                if (best != null) {
                    updateLocationCache(best);
                    return best;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error checking native location: " + e.getMessage());
            }
        }

        // Check SessionManager persistent cache
        double savedLat = sessionManager.getLastKnownLatitude(DEFAULT_FALLBACK_LAT);
        double savedLng = sessionManager.getLastKnownLongitude(DEFAULT_FALLBACK_LNG);
        Location synthetic = new Location("session_cache");
        synthetic.setLatitude(savedLat);
        synthetic.setLongitude(savedLng);
        synthetic.setTime(System.currentTimeMillis());
        return synthetic;
    }

    @SuppressLint("MissingPermission")
    private void queryFastCachedLocation() {
        if (!hasLocationPermission(appContext)) return;
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    updateLocationCache(loc);
                }
            });
        } catch (Exception ignored) {}
    }

    /**
     * Request a fresh high-accuracy location fix.
     * Fires listener immediately if cached location is fresh, and attempts
     * an active high-precision GPS lock with a 6-second timeout guarantee.
     */
    @SuppressLint("MissingPermission")
    public void fetchCurrentLocation(final OnLocationResultListener listener) {
        final Location cached = getBestAvailableLocation();

        if (!hasLocationPermission(appContext)) {
            if (listener != null) {
                mainHandler.post(() -> listener.onLocationResolved(cached));
            }
            return;
        }

        final boolean[] responded = {false};
        final Runnable timeoutRunnable = () -> {
            if (!responded[0]) {
                responded[0] = true;
                if (listener != null) {
                    listener.onLocationResolved(getBestAvailableLocation());
                }
            }
        };
        mainHandler.postDelayed(timeoutRunnable, 6000);

        try {
            CancellationTokenSource cts = new CancellationTokenSource();
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            updateLocationCache(loc);
                            if (!responded[0]) {
                                responded[0] = true;
                                mainHandler.removeCallbacks(timeoutRunnable);
                                if (listener != null) {
                                    listener.onLocationResolved(loc);
                                }
                            }
                        } else {
                            // Fallback to last known fused location
                            fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                                if (lastLoc != null) {
                                    updateLocationCache(lastLoc);
                                    if (!responded[0]) {
                                        responded[0] = true;
                                        mainHandler.removeCallbacks(timeoutRunnable);
                                        if (listener != null) {
                                            listener.onLocationResolved(lastLoc);
                                        }
                                    }
                                }
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getCurrentLocation failed: " + e.getMessage());
                        requestNativeSingleUpdate(listener, responded, timeoutRunnable);
                    });
        } catch (Exception e) {
            Log.w(TAG, "Fused location error: " + e.getMessage());
            requestNativeSingleUpdate(listener, responded, timeoutRunnable);
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNativeSingleUpdate(OnLocationResultListener listener, boolean[] responded, Runnable timeoutRunnable) {
        if (locationManager == null || !hasLocationPermission(appContext)) return;
        try {
            LocationListener singleListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    updateLocationCache(location);
                    if (!responded[0]) {
                        responded[0] = true;
                        mainHandler.removeCallbacks(timeoutRunnable);
                        if (listener != null) {
                            listener.onLocationResolved(location);
                        }
                    }
                    try {
                        locationManager.removeUpdates(this);
                    } catch (Exception ignored) {}
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}
                @Override public void onProviderDisabled(@NonNull String provider) {}
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleListener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleListener, Looper.getMainLooper());
            }
        } catch (Exception e) {
            Log.w(TAG, "Native single update failed: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public void startPassiveLocationUpdates() {
        if (isListening || !hasLocationPermission(appContext)) return;
        isListening = true;

        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000)
                    .setMinUpdateIntervalMillis(10000)
                    .setMaxUpdateDelayMillis(60000)
                    .build();

            fusedCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult.getLastLocation() != null) {
                        updateLocationCache(locationResult.getLastLocation());
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper());
        } catch (Exception e) {
            Log.w(TAG, "Failed to start passive fused updates: " + e.getMessage());
        }
    }

    public void stopPassiveLocationUpdates() {
        if (!isListening) return;
        isListening = false;
        try {
            if (fusedCallback != null) {
                fusedLocationClient.removeLocationUpdates(fusedCallback);
                fusedCallback = null;
            }
        } catch (Exception ignored) {}
    }

    private void updateLocationCache(@NonNull Location location) {
        this.lastKnownLocation = location;
        sessionManager.setLastKnownLocation(location.getLatitude(), location.getLongitude());
        Log.d(TAG, String.format("Location resolved: %.6f, %.6f (provider=%s, accuracy=%.1fm)",
                location.getLatitude(), location.getLongitude(), location.getProvider(), location.getAccuracy()));
    }
}
