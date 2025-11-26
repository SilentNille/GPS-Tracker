package com.example.gps_tracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.gps_tracker.vorgeben.LatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private GpsGraphView gpsGraphView;
    private Button startPauseButton, clearButton, showCsvButton;
    private ImageButton downloadGpxButton;
    private TextView latitudeValue, longitudeValue, altitudeValue;

    private boolean tracking = false;
    private LocationManager locationManager;

    private AlertDialog csvDialog;
    private TextView csvDataTextView;

    private static final String CSV_HEADER = "time,latitude,longitude,altitude\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpsGraphView = findViewById(R.id.gpsGraphView);
        startPauseButton = findViewById(R.id.startPauseButton);
        clearButton = findViewById(R.id.clearButton);
        showCsvButton = findViewById(R.id.showCsvButton);
        downloadGpxButton = findViewById(R.id.downloadGpxButton);
        latitudeValue = findViewById(R.id.latitudeValue);
        longitudeValue = findViewById(R.id.longitudeValue);
        altitudeValue = findViewById(R.id.altitudeValue);

        startPauseButton.setOnClickListener(v -> {
            if (tracking) {
                pauseTracking();
            } else {
                startTracking();
            }
        });

        clearButton.setOnClickListener(v -> {
            clearTrackingData();
        });

        showCsvButton.setOnClickListener(v -> {
            showCsvDataDialog();
        });

        downloadGpxButton.setOnClickListener(v -> {
            try {
                Log.d("CSV","Try write xml");
                GPXConverter.convertAndDownloadGPXFromCSV(this);
                Toast.makeText(this, "GPX file downloaded.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error downloading GPX file.", Toast.LENGTH_SHORT).show();
                throw new RuntimeException(e);
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    private void startTracking() {
        tracking = true;
        startPauseButton.setText("Pause");
        startLocationUpdates();
    }

    private void pauseTracking() {
        tracking = false;
        startPauseButton.setText("Start");
        stopLocationUpdates();
    }

    private void clearTrackingData() {
        gpsGraphView.clearTrack();
        File file = new File(getFilesDir(), "gps_data.csv");
        if (file.exists()) {
            file.delete();
        }
        if (csvDialog != null && csvDialog.isShowing()) {
            csvDataTextView.setText("No CSV data found.");
        }
    }

    private void showCsvDataDialog() {
        if (csvDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("CSV Data");

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_csv, null);
            builder.setView(dialogView);
            csvDataTextView = dialogView.findViewById(R.id.csvDataTextView);

            builder.setPositiveButton("OK", (dialog, which) -> {
                csvDialog.dismiss();
            });
            csvDialog = builder.create();
        }

        updateCsvDialog();
        csvDialog.show();
    }

    private void updateCsvDialog() {
        File file = new File(getFilesDir(), "gps_data.csv");
        StringBuilder csvData = new StringBuilder();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    csvData.append(line).append("\n");
                }
            } catch (IOException e) {
                csvData.append("Error reading CSV file.");
            }
        } else {
            csvData.append("No CSV data found.");
        }

        if(csvDataTextView != null) {
            csvDataTextView.setText(csvData.toString());
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
        }
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        latitudeValue.setText(String.format(Locale.US, "%.6f", latitude));
        longitudeValue.setText(String.format(Locale.US, "%.6f", longitude));

        if (location.hasAltitude()) {
            double altitude = location.getAltitude();
            altitudeValue.setText(String.format(Locale.US, "%.2f m", altitude));
        } else {
            altitudeValue.setText("N/A");
        }

        LatLng latLng = new LatLng(latitude, longitude);
        gpsGraphView.addPoint(latLng);

        long time = location.getTime();
        String csvLine = String.format(Locale.US, "%d,%.6f,%.6f,%.2f", time, latitude, longitude, location.getAltitude());
        writeToCsv(csvLine);

        if (csvDialog != null && csvDialog.isShowing()) {
            updateCsvDialog();
        }
    }

    private void writeToCsv(String line) {
        File file = new File(getFilesDir(), "gps_data.csv");
        try (FileWriter fw = new FileWriter(file, true)) {
            if (file.length() == 0) {
                fw.write(CSV_HEADER + System.lineSeparator());
            }
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tracking) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tracking) {
            stopLocationUpdates();
        }
    }
}
