package com.example.gps_tracker;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GPXConverter {

    private static final String TAG = "GPXConverter";

    public static void convertAndDownloadGPXFromCSV(Context context) throws IOException {
        File csvFile = new File(context.getFilesDir(), "gps_data.csv");
        Log.d(TAG, "Attempting to read from: " + csvFile.getAbsolutePath());

        if (!csvFile.exists()) {
            Log.e(TAG, "CSV file does not exist.");
            return;
        }
        if (csvFile.length() == 0) {
            Log.w(TAG, "CSV file is empty.");
            return;
        }
        Log.d(TAG, "CSV file found with size: " + csvFile.length() + " bytes.");

        StringBuilder gpxContent = new StringBuilder();
        gpxContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n");
        gpxContent.append("<gpx version=\"1.1\" creator=\"GPS-Tracker\">\n");
        gpxContent.append("  <trk>\n");
        gpxContent.append("    <name>Track</name>\n");
        gpxContent.append("    <trkseg>\n");

        int linesRead = 0;
        int pointsAdded = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            // Skip header
            String header = reader.readLine();
            Log.d(TAG, "Skipped header line: " + header);

            while ((line = reader.readLine()) != null) {
                linesRead++;
                Log.d(TAG, "Read line (" + linesRead + "): " + line);

                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    pointsAdded++;
                    Log.d(TAG, "Line " + linesRead + " parsed into " + parts.length + " parts: " + Arrays.toString(parts));
                    gpxContent.append("      <trkpt lat=\"").append(parts[1]).append("\" lon=\"").append(parts[2]).append("\">\n");
                    gpxContent.append("        <ele>").append(parts[3]).append("</ele>\n");
                    try {
                        long time = Long.parseLong(parts[0]);
                        Date date = new Date(time);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String formattedTime = sdf.format(date);
                        gpxContent.append("        <time>").append(formattedTime).append("</time>\n");
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing timestamp on line " + linesRead, e);
                        gpxContent.append("        <time>").append(parts[0]).append("</time>\n");
                    }
                    gpxContent.append("      </trkpt>\n");
                } else {
                    Log.w(TAG, "Skipping malformed line " + linesRead + " (only " + parts.length + " parts): " + line);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while reading CSV file", e);
        }

        gpxContent.append("    </trkseg>\n");
        gpxContent.append("  </trk>\n");
        gpxContent.append("</gpx>\n");

        Log.d(TAG, "Finished processing CSV. Total lines read (excluding header): " + linesRead + ". Track points added: " + pointsAdded);
        if (pointsAdded == 0 && linesRead > 0) {
            Log.w(TAG, "No track points were added, even though CSV file has content. Check for formatting issues.");
        }
        Log.d(TAG, "Final GPX Content Snippet: " + gpxContent.substring(0, Math.min(gpxContent.length(), 400)));


        saveGpxFile(context, gpxContent.toString());
    }

    private static void saveGpxFile(Context context, String gpxContent) throws IOException {
        ContentValues values = new ContentValues();
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US).format(new Date());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "track-" + formattedDate + ".gpx");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/gpx+xml");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            Log.d(TAG, "Attempting to save to URI: " + uri.toString());
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(gpxContent.getBytes());
                    Log.d(TAG, "Successfully wrote GPX content to file.");
                } else {
                    Log.e(TAG, "Failed to open OutputStream for URI: " + uri.toString());
                    throw new IOException("Failed to get output stream.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing to GPX file.", e);
                throw e;
            }
        } else {
            Log.e(TAG, "Failed to create MediaStore entry for the GPX file.");
            throw new IOException("Failed to create new MediaStore record.");
        }
    }
}
