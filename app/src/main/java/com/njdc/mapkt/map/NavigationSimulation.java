package com.njdc.mapkt.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

public class NavigationSimulation {
    private static final long STEP_INTERVAL = 1000 / 60; // second
    private static final double LOW_SPEED = 0.5; // meter per second
    private static final double MEDIUM_SPEED = 1.0;
    private static final double HIGH_SPEED = 2.0;
    private double v;

    private NavigationSimulationListener updateListener;

    private Double[][] roadPoint;

    private int startIndex;
    private double pathDistance;
    private double roadSin;
    private double roadCos;

    private double passedDistance;

/*    public NavigationSimulation(long poiFrom, long poiTo, NavigationSimulationListener listener, MapManager mapManager) {
        updateListener = listener;
        setNormalSpeed();
        String roadString = mapManager.searchRoute2(poiFrom, poiTo);
        parseRoute(roadString);
    }*/

    public NavigationSimulation(String path, NavigationSimulationListener listener) {
        updateListener = listener;
        setNormalSpeed();
        parseRoute(path);
    }

    private Timer timer = new Timer();

    public void setLowSpeed() {
        setSpeed(LOW_SPEED);
    }

    public void setHighSpeed() {
        setSpeed(HIGH_SPEED);
    }

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            passedDistance += v * STEP_INTERVAL / 1000.0;
            Timber.d("passedDistance: %f, pathDistance: %f", passedDistance, pathDistance);
            if (passedDistance >= pathDistance) {
                passedDistance -= pathDistance;
                startIndex++;
                if (!preparePathParameters()) {
                    timer.cancel();
                    double[] coordinate = {roadPoint[startIndex][0], roadPoint[startIndex][1]};
                    updateListener.update(true, coordinate);
                    Timber.d("RES_COMPLETED, longitude: %f, latitude: %f", coordinate[0], coordinate[1]);
                    return;
                }
            }
            double lon = roadPoint[startIndex][0] + passedDistance * roadCos;
            double lat = roadPoint[startIndex][1] + passedDistance * roadSin;
            double[] coordinate = {lon, lat};
            updateListener.update(false, coordinate);

            Timber.d("RES_UPDATE, longitude: %f, latitude: %f", lon, lat);
        }
    };

    private void setNormalSpeed() {
        setSpeed(MEDIUM_SPEED);
    }

    private void setSpeed(double speed) {
        v = speed / 1000; // v Kilometer per second
        v /= 111.19492664455873734580833886041; //degree per second
    }

    public void pause() {
        timer.cancel();
    }

    public void goAhead() {
        timer.scheduleAtFixedRate(timerTask, 0, STEP_INTERVAL);
    }

    public void stop() {
        timer.cancel();
        startIndex = 0;
        passedDistance = 0;
    }

    public void start() {
        //timer.cancel();
        startIndex = 0;
        passedDistance = 0;
        if (preparePathParameters()) {
            timer.scheduleAtFixedRate(timerTask, 0, STEP_INTERVAL);
        } else {
            double[] coordinate = {roadPoint[startIndex][0], roadPoint[startIndex][1]};
            updateListener.update(true, coordinate);
        }
    }

    private void parseRoute(String roadString) {
        Timber.d(roadString);
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(roadString);
        JsonObject jo = je.getAsJsonObject();
        JsonArray ja = jo.getAsJsonArray("features").get(0).getAsJsonObject().getAsJsonObject("geometry").getAsJsonArray("coordinates");
        roadPoint = new Double[ja.size()][2];
        for (int i = 0; i < ja.size(); i++) {
            JsonElement je1 = ja.get(i);
            JsonArray ja1 = je1.getAsJsonArray();
            roadPoint[i][0] = ja1.get(0).getAsDouble();
            roadPoint[i][1] = ja1.get(1).getAsDouble();
        }

        Timber.d("roadPoint count: %d", roadPoint.length);
        for (int i = 0; i < roadPoint.length; i++) {
            Timber.d("%d, longitude: %f, latitude: %f", i, roadPoint[i][0], roadPoint[i][1]);
        }
    }

    private boolean preparePathParameters() {
        int endIndex = startIndex + 1;
        if (endIndex >= roadPoint.length) {
            return false;
        }
        double startLon = roadPoint[startIndex][0];
        double startLat = roadPoint[startIndex][1];
        double endLon = roadPoint[endIndex][0];
        double endLat = roadPoint[endIndex][1];

        double deltLon = endLon - startLon;
        double deltLat = endLat - startLat;
        pathDistance = Math.sqrt(deltLon * deltLon + deltLat * deltLat);
        roadSin = deltLat / pathDistance;
        roadCos = deltLon / pathDistance;
        return true;
    }

    public interface NavigationSimulationListener {
        void update(boolean completed, double[] coordinate);
    }
}
