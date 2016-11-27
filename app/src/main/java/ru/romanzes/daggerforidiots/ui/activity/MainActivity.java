package ru.romanzes.daggerforidiots.ui.activity;

import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ru.romanzes.daggerforidiots.R;
import ru.romanzes.daggerforidiots.util.LocationWorker;

public class MainActivity extends AppCompatActivity implements LocationWorker.EventListener {
    private ProgressBar progress;
    private TextView tvMessage;
    private Button btnRetry;

    private LocationWorker locationWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = (ProgressBar) findViewById(R.id.progress);
        tvMessage = (TextView) findViewById(R.id.tv_message);
        btnRetry = (Button) findViewById(R.id.btn_retry);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgress();
                locationWorker.start();
            }
        });

        locationWorker = new LocationWorker(this);
        locationWorker.setEventListener(this);
    }

    @Override
    protected void onStart() {
        locationWorker.start();
        super.onStart();
    }

    @Override
    protected void onStop() {
        locationWorker.stop();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        locationWorker.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationWorker.handleRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onUserDisallowedLocationEnabling() {
        showErrorMessage("Location is disabled");
    }

    @Override
    public void onUserDeniedLocationPermission() {
        showErrorMessage("Permission denied");
    }

    @Override
    public void onPlayServicesConnectionSuspended(int reason) {
        showErrorMessage("Connection suspended");
    }

    @Override
    public void onPlayServicesConnectionFailed() {
        showErrorMessage("Connection failed");
    }

    @Override
    public void onLocationChangeUnavailable() {
        showErrorMessage("Location change unavailable");
    }

    @Override
    public void onLocationObtained(Location location) {
        showCoordinates(location);
    }

    private void showProgress() {
        progress.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
    }

    private void showErrorMessage(String message) {
        progress.setVisibility(View.GONE);
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.VISIBLE);
    }

    private void showCoordinates(Location location) {
        progress.setVisibility(View.GONE);
        tvMessage.setText(location.getLatitude() + ", " + location.getLongitude());
        tvMessage.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
    }
}
