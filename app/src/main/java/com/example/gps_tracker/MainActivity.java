package com.example.gps_tracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final double SEA_LEVEL_PRESSURE_HPA = 1013.25;
    private static final double ALTITUDE_CONSTANT = 44330.0;
    private static final double EXPONENT = 0.1903;
    private double currentLongitude, currentLatitude, currentAltitude;
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.d("SensorCheck", "Kein Drucksensor");
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
    }
    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    10,
                    location -> {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                    }
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float pressure = event.values[0];
        currentAltitude = ALTITUDE_CONSTANT * (1.0 - Math.pow(pressure / SEA_LEVEL_PRESSURE_HPA, EXPONENT));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}