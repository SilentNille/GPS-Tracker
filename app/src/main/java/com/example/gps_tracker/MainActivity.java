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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String CSV_FORMAT = "%s,%s,%s,%s"; // Uhrzeit, Längengrad, Breitengrad, Höhe
    private static final long THREAD_SLEEP = 2000;
    private static final double SEA_LEVEL_PRESSURE_HPA = 1013.25;
    private static final double ALTITUDE_CONSTANT = 44330.0;
    private static final double EXPONENT = 0.1903;
    private double currentLongitude, currentLatitude, currentAltitude;
    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private LocationManager locationManager;
    private boolean stopCSVShedular = false;
    private boolean isRunning = false;
    private Thread csvShedular = new Thread(() -> {
        isRunning = true;
        while(true){
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            String csvLine = String.format(CSV_FORMAT, currentTime, currentLongitude, currentLatitude, currentAltitude);

            if(stopCSVShedular){
                break;
            }

            File file = new File(getFilesDir(), "gps_data.csv");
            boolean writeLine = true;

            if(file.exists()){
                try(BufferedReader reader = new BufferedReader(new FileReader(file))){
                    String lastLine = null, line;
                    while((line = reader.readLine()) != null){
                        lastLine = line;
                    }
                    if(lastLine != null && lastLine.equals(csvLine.trim())){
                        writeLine = false;
                    }
                } catch(IOException e){
                    e.printStackTrace();
                }
            }

            if(writeLine){
                try(FileWriter fw = new FileWriter(file, true)){
                    fw.write(csvLine + "\n");
                } catch(IOException e){
                    e.printStackTrace();
                }
            }

            try{
                Thread.sleep(THREAD_SLEEP);
            } catch(InterruptedException e){
               e.printStackTrace();
            }
        }
        isRunning = false;
    });

    private void clearCsv() {
        File file = new File(getFilesDir(), "gps_data.csv");
        if(file.exists()){
            boolean deleted = file.delete();
            if(deleted){
                Log.d("CSV", "Datei gelöscht");
            } else {
                Log.d("CSV", "Datei konnte nicht gelöscht werden");
            }
        }
    }

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

        //Write every new Line to CSV
        csvShedular.start();
    }
    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    THREAD_SLEEP,
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

    @Override
    protected void onPause() {
        super.onPause();
        stopCSVShedular = true;
        Log.d("CSV","Try to Stop the Shedular");
    }

    @Override
    protected void onResume() {
        super.onResume();
        while(isRunning && stopCSVShedular){
            try {
                Thread.sleep(100);
                Log.d("CSV","Waiting for old Shedular Thread to finish before resuming");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        if(!isRunning) {
            stopCSVShedular = false;
            csvShedular.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCSVShedular = true;
        clearCsv();
    }
}