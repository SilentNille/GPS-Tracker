package com.example.gps_tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.gps_tracker.vorgeben.LatLng;

import java.util.ArrayList;
import java.util.List;

public class GpsGraphView extends View {

    private Paint gridPaint;
    private Paint trackPaint;
    private Path trackPath;
    private List<LatLng> latLngPoints = new ArrayList<>();

    public GpsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);

        trackPaint = new Paint();
        trackPaint.setColor(Color.BLUE);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(5);
        trackPaint.setAntiAlias(true);

        trackPath = new Path();
    }

    public void addPoint(LatLng point) {
        latLngPoints.add(point);
        updatePath();
        invalidate();
    }

    public void clearTrack() {
        latLngPoints.clear();
        trackPath.reset();
        invalidate();
    }

    public List<LatLng> getLatLngPoints() {
        return latLngPoints;
    }

    private void updatePath() {
        trackPath.reset();
        if (latLngPoints.size() < 2) {
            return;
        }

        double minLatitude = Double.MAX_VALUE;
        double maxLatitude = Double.MIN_VALUE;
        double minLongitude = Double.MAX_VALUE;
        double maxLongitude = Double.MIN_VALUE;

        for (LatLng point : latLngPoints) {
            if (point.getLat() < minLatitude) minLatitude = point.getLat();
            if (point.getLat() > maxLatitude) maxLatitude = point.getLat();
            if (point.getLng() < minLongitude) minLongitude = point.getLng();
            if (point.getLng() > maxLongitude) maxLongitude = point.getLng();
        }

        double longitudeRange = maxLongitude - minLongitude;
        double latitudeRange = maxLatitude - minLatitude;
        double geoSize = Math.max(longitudeRange, latitudeRange);

        if (geoSize == 0) {
            geoSize = 1;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float size = Math.min(viewWidth, viewHeight);

        float xOffset = (viewWidth - size) / 2f;
        float yOffset = (viewHeight - size) / 2f;

        trackPath.reset();

        LatLng firstPoint = latLngPoints.get(0);

        float x0 = xOffset + (float) ((firstPoint.getLng() - minLongitude) / geoSize * size);
        float y0_prime = (float) ((firstPoint.getLat() - minLatitude) / geoSize * size);
        float y0 = yOffset + size - y0_prime;

        trackPath.moveTo(x0, y0);

        for (int i = 1; i < latLngPoints.size(); i++) {
            LatLng currentPoint = latLngPoints.get(i);
            float x = xOffset + (float) ((currentPoint.getLng() - minLongitude) / geoSize * size);
            float y_prime = (float) ((currentPoint.getLat() - minLatitude) / geoSize * size);
            float y = yOffset + size - y_prime;
            trackPath.lineTo(x, y);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        canvas.drawPath(trackPath, trackPaint);
    }

    private void drawGrid(Canvas canvas) {
        int gridSize = 5;
        float cellWidth = (float) getWidth() / gridSize;
        float cellHeight = (float) getHeight() / gridSize;

        for (int i = 1; i < gridSize; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }

        for (int i = 1; i < gridSize; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }
}
