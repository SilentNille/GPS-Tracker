package com.example.gps_tracker;

import android.content.Context;
import android.os.Environment;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GPXConverter {
    private record Trackerpoint(float longitude, float latitude){}
    private static String[] loadCsvLines(Context context) {
        File csvFile = new File(context.getFilesDir(), "gps_data.csv");
        if (!csvFile.exists()) return new String[0];

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines.toArray(new String[0]);
    }

    public static void convertAndDownloadGPXFromCSV(Context context) throws IOException {
        String[] linesToConvert = loadCsvLines(context);
        File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir == null) return;
        File gpxFile = new File(downloadDir, "gps_data.gpx");

        try (FileOutputStream fos = new FileOutputStream(gpxFile)) {
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "gpx")
                    .attribute("", "version", "1.1")
                    .attribute("", "creator", "Patrick und Philip");
            serializer.startTag("", "trk");
            serializer.startTag("", "trkseg");
            for (String line : linesToConvert) {
                Trackerpoint trackerpoint = lineToTrackerPoint(line);
                serializer.startTag("","trkpt");
                serializer.attribute("","lat", trackerpoint.latitude + "");
                serializer.attribute("","lon", trackerpoint.longitude + "");
                serializer.endTag("","trkpt");
            }
            serializer.endTag("", "trkseg");
            serializer.endTag("", "trk");
            serializer.endTag("", "gpx");
            serializer.endDocument();
            serializer.flush();
        }
    }

    private static Trackerpoint lineToTrackerPoint(String line) {
        String[] segments = line.split(",");
        return new Trackerpoint(Float.valueOf(segments[1]),Float.valueOf(segments[2]));
    }
}
