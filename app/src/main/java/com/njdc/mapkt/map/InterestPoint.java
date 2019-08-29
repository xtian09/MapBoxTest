package com.njdc.mapkt.map;

import com.cocoahero.android.geojson.Position;

public class InterestPoint {
    public long id;
    public String text;
    public long imageId;
    public double floorId;
    public String geoJsonString;

    public Position position;

    public double[] getCoordinate() {
        return position.toArray();
    }
}
