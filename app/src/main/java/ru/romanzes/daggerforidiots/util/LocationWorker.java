package ru.romanzes.daggerforidiots.util;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import ru.romanzes.daggerforidiots.Constants;

public class LocationWorker implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private Activity activity;

    private GoogleApiClient googleApiClient;

    private EventListener listener;

    public LocationWorker(Activity activity) {
        this.activity = activity;
        googleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void setEventListener(EventListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (googleApiClient.isConnected()) {
            requestLocationPermission();
        } else {
            googleApiClient.connect();
        }
    }

    public void stop() {
        googleApiClient.disconnect();
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RequestCode.ENABLE_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                startLocationUpdates();
            } else {
                if (listener != null) {
                    listener.onUserDisallowedLocationEnabling();
                }
            }
        }
    }

    public void handleRequestPermissionResult(int requestCode, @NonNull String[] permissions,
                                              @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.RequestCode.LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    obtainLocation();
                } else {
                    if (listener != null) {
                        listener.onUserDeniedLocationPermission();
                    }
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationPermission();
    }


    @Override
    public void onConnectionSuspended(int reason) {
        // TODO we probably should try to reconnect in this case
        if (listener != null) {
            listener.onPlayServicesConnectionSuspended(reason);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO we probably should try to reconnect in this case
        if (listener != null) {
            listener.onPlayServicesConnectionFailed();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        triggerLocationListener(location);
    }

    private void requestLocationPermission() {
        boolean coarseGranted = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean fineGranted = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (coarseGranted && fineGranted) {
            obtainLocation();
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    Constants.RequestCode.LOCATION_PERMISSION);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void obtainLocation() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            triggerLocationListener(location);
        }
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(Constants.LOCATION_UPDATE_INTERVAL);
        locationRequest.setInterval(Constants.LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        checkSettings(locationRequest);
    }

    private void checkSettings(final LocationRequest locationRequest) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(googleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        requestLocationUpdates(locationRequest);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(activity,
                                    Constants.RequestCode.ENABLE_LOCATION);
                        } catch (IntentSender.SendIntentException ignored) {}
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        if (listener != null) {
                            listener.onLocationChangeUnavailable();
                        }
                        break;
                }
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates(LocationRequest locationRequest) {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest,
                this);
    }

    private void triggerLocationListener(Location location) {
        if (listener != null) {
            listener.onLocationObtained(location);
        }
    }

    public interface EventListener {
        void onUserDisallowedLocationEnabling();
        void onUserDeniedLocationPermission();
        void onPlayServicesConnectionSuspended(int reason);
        void onPlayServicesConnectionFailed();
        void onLocationChangeUnavailable();
        void onLocationObtained(Location location);
    }
}
