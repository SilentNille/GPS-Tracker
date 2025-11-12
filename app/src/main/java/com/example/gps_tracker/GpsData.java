package com.example.gps_tracker;

public class GpsData {
    String time;
    double longitude;
    double latitude;
    double altitude;

    public GpsData(String time, double longitude, double latitude, double altitude) {
        this.time = time;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
    }
}
