package com.example.gps_tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.gps_tracker.vorgeben.LatLng;
import com.example.gps_tracker.vorgeben.UTMRef;

import java.util.ArrayList;
import java.util.List;

public class GpsGraphView extends View {

    private Paint gridPaint;
    private Paint trackPaint;
    private Paint axisLabelPaint;
    private Paint axisTitlePaint;
    private Paint directionPointerPaint;

    private List<LatLng> latLngPoints = new ArrayList<>();
    private List<UTMRef> utmPoints = new ArrayList<>();
    private float phoneBearing = 0f;

    public GpsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        trackPaint = new Paint();
        trackPaint.setColor(Color.BLUE);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(5);
        trackPaint.setAntiAlias(true);

        axisLabelPaint = new Paint();
        axisLabelPaint.setColor(Color.BLACK);
        axisLabelPaint.setTextSize(24f);
        axisLabelPaint.setAntiAlias(true);

        axisTitlePaint = new Paint();
        axisTitlePaint.setColor(Color.DKGRAY);
        axisTitlePaint.setTextSize(30f);
        axisTitlePaint.setAntiAlias(true);
        axisTitlePaint.setFakeBoldText(true);

        directionPointerPaint = new Paint();
        directionPointerPaint.setColor(Color.RED);
        directionPointerPaint.setStyle(Paint.Style.FILL);
        directionPointerPaint.setAntiAlias(true);
    }

    public void addPoint(LatLng point) {
        latLngPoints.add(point);
        utmPoints.add(point.toUTMRef());
        invalidate();
    }

    public void setPoints(List<LatLng> points) {
        latLngPoints.clear();
        utmPoints.clear();
        if (points != null) {
            latLngPoints.addAll(points);
            for (LatLng point : points) {
                utmPoints.add(point.toUTMRef());
            }
        }
        invalidate();
    }

    public void updatePhoneBearing(float bearing) {
        this.phoneBearing = bearing;
        invalidate();
    }

    public void clearTrack() {
        latLngPoints.clear();
        utmPoints.clear();
        invalidate();
    }

    public List<LatLng> getLatLngPoints() {
        return latLngPoints;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (utmPoints.isEmpty()) {
            drawPlaceholder(canvas);
            return;
        }

        double minEasting = Double.MAX_VALUE;
        double maxEasting = Double.MIN_VALUE;
        double minNorthing = Double.MAX_VALUE;
        double maxNorthing = Double.MIN_VALUE;

        for (UTMRef point : utmPoints) {
            minEasting = Math.min(minEasting, point.getEasting());
            maxEasting = Math.max(maxEasting, point.getEasting());
            minNorthing = Math.min(minNorthing, point.getNorthing());
            maxNorthing = Math.max(maxNorthing, point.getNorthing());
        }

        double eastingRange = maxEasting - minEasting;
        double northingRange = maxNorthing - minNorthing;

        if (utmPoints.size() == 1) {
            eastingRange = 100; // Default range for a single point
            northingRange = 100;
            minEasting -= 50;
            minNorthing -= 50;
        }

        double utmSize = Math.max(eastingRange, northingRange);
        if (utmSize == 0) utmSize = 100;


        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float canvasPadding = 80f; // Space for labels
        float size = Math.min(viewWidth, viewHeight) - (2 * canvasPadding);
        float xOffset = canvasPadding;
        float yOffset = canvasPadding;


        drawGridAndLabels(canvas, minEasting, minNorthing, utmSize, size, xOffset, yOffset);


        if (utmPoints.size() >= 1) {
            drawTrack(canvas, minEasting, minNorthing, utmSize, size, xOffset, yOffset);
            drawDirectionPointer(canvas, minEasting, minNorthing, utmSize, size, xOffset, yOffset);
        }
    }

    private void drawPlaceholder(Canvas canvas) {
        canvas.drawText("Waiting for GPS data...", getWidth() / 2f - 200, getHeight() / 2f, axisTitlePaint);
    }

    private void drawGridAndLabels(Canvas canvas, double minEasting, double minNorthing, double utmSize, float size, float xOffset, float yOffset) {
        int gridSize = 5;
        canvas.drawText("Northing (m)", xOffset, yOffset - 40, axisTitlePaint);
        canvas.drawText("Easting (m)", getWidth() / 2f - 100, yOffset + size + 70, axisTitlePaint);

        for (int i = 0; i <= gridSize; i++) {
            float ratio = (float) i / gridSize;

            // Vertical lines and Easting labels
            float x = xOffset + ratio * size;
            canvas.drawLine(x, yOffset, x, yOffset + size, gridPaint);
            double easting = minEasting + (ratio * utmSize);
            canvas.drawText(String.format("%.0f", easting), x, yOffset + size + 40, axisLabelPaint);


            float y = yOffset + size - (ratio * size);
            canvas.drawLine(xOffset, y, xOffset + size, y, gridPaint);
            double northing = minNorthing + (ratio * utmSize);
            canvas.save();
            canvas.rotate(-90, xOffset - 20, y);
            canvas.drawText(String.format("%.0f", northing), xOffset - 20, y, axisLabelPaint);
            canvas.restore();
        }
    }

    private void drawTrack(Canvas canvas, double minEasting, double minNorthing, double utmSize, float size, float xOffset, float yOffset) {
        if(utmPoints.size() < 2) return;
        Path trackPath = new Path();

        UTMRef firstPoint = utmPoints.get(0);
        float x0 = xOffset + (float) ((firstPoint.getEasting() - minEasting) / utmSize * size);
        float y0_prime = (float) ((firstPoint.getNorthing() - minNorthing) / utmSize * size);
        float y0 = yOffset + size - y0_prime;
        trackPath.moveTo(x0, y0);

        for (int i = 1; i < utmPoints.size(); i++) {
            UTMRef currentPoint = utmPoints.get(i);
            float x = xOffset + (float) ((currentPoint.getEasting() - minEasting) / utmSize * size);
            float y_prime = (float) ((currentPoint.getNorthing() - minNorthing) / utmSize * size);
            float y = yOffset + size - y_prime;
            trackPath.lineTo(x, y);
        }
        canvas.drawPath(trackPath, trackPaint);
    }

    private void drawDirectionPointer(Canvas canvas, double minEasting, double minNorthing, double utmSize, float size, float xOffset, float yOffset) {
        if (utmPoints.isEmpty()) return;

        UTMRef lastPoint = utmPoints.get(utmPoints.size() - 1);

        float x = xOffset + (float) ((lastPoint.getEasting() - minEasting) / utmSize * size);
        float y_prime = (float) ((lastPoint.getNorthing() - minNorthing) / utmSize * size);
        float y = yOffset + size - y_prime;

        Path arrow = new Path();
        arrow.moveTo(0, -30); // Point of the arrow
        arrow.lineTo(-20, 20);
        arrow.lineTo(0, 10);
        arrow.lineTo(20, 20);
        arrow.close();

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(phoneBearing);
        canvas.drawPath(arrow, directionPointerPaint);
        canvas.restore();
    }
}
