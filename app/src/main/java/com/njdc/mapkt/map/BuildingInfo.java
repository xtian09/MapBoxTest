package com.njdc.mapkt.map;

import java.util.ArrayList;
import java.util.List;

public class BuildingInfo {
    private String buildingId;
    private String geoJsonString;
    private String poiJsonString;
    private List<Double> floorIds = new ArrayList<>();
    private List<String> floorNames = new ArrayList<>();

    BuildingInfo(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setGeoJsonString(String geoJsonString) {
        this.geoJsonString = geoJsonString;
    }

    public String getGeoJsonString() {
        return geoJsonString;
    }

    public void setPoiJsonString(String goiJsonString) {
        this.poiJsonString = goiJsonString;
    }

    public String getPoiJsonString() {
        return poiJsonString;
    }

    public void addFloor(double floorId, String floorName) {
        floorIds.add(floorId);
        floorNames.add(floorName);
    }

    public int getFloorCount() {
        return floorIds.size();
    }

    public double getFloorId(int i) {
        return floorIds.get(i);
    }

    public String getFloorName(int i) {
        return floorNames.get(i);
    }

    public List<Double> getFloorIds() {
        return floorIds;
    }

    public void setFloorIds(List<Double> floorIds) {
        this.floorIds = floorIds;
    }
}
