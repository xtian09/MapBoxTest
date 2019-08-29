package com.njdc.mapkt.map;

import android.os.Environment;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.Geometry;
import com.cocoahero.android.geojson.GeometryCollection;
import com.cocoahero.android.geojson.LineString;
import com.cocoahero.android.geojson.MultiLineString;
import com.cocoahero.android.geojson.MultiPoint;
import com.cocoahero.android.geojson.MultiPolygon;
import com.cocoahero.android.geojson.Point;
import com.cocoahero.android.geojson.Polygon;
import com.cocoahero.android.geojson.Position;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import timber.log.Timber;

public class MapManager {
    private static final String GeoTable = "DF12";
    private static final String PoiTable = "DF12_2F_TEXT";
    private static final String RoadNodeTable = "DF12_2F_ROAD2_ND2_Junctions";
    private static final String RoadTable = "DF12_2F_ROAD";
    private static final String RoadSearchTable = "DF12_2F_ROAD_net";

    private Database database;

    public MapManager(String dbPath) {
        Timber.d(dbPath);
        try {
            database = new Database();
            database.open(dbPath, Constants.SQLITE_OPEN_READONLY);
            spatialiteVersion();
        } catch (Exception e) {
            e.printStackTrace();
        }

/*        String path = searchPath(new double[]{113.8361340261644,34.55843169666553}, new double[]{113.8370315450698,34.55810688942148});
        NavigationSimulation ns = new NavigationSimulation(path, new NavigationSimulation.NavigationSimulationListener() {
            @Override
            public void update(boolean completed, double[] coordinate) {
                Timber.d("update, complete: %b, coordiante(%f, %f)", completed, coordinate[0], coordinate[1]);
            }
        });
        ns.start();*/
    }

    public void close() throws Exception {
        database.close();
    }

    public BuildingInfo getBuildingInfo(String buildingId) {
        BuildingInfo bi = new BuildingInfo(buildingId);
        bi.setGeoJsonString(getMapJsonString(buildingId));
        bi.setPoiJsonString(getPoiJsonString(buildingId));
        List<Double> floorIds = getFloorIds(buildingId);
        for (int i=0; i<floorIds.size(); i++) {
            bi.addFloor(floorIds.get(i), cutRedundantZeroAndDot(""+floorIds.get(i))+" F");
        }
        return bi;
    }

    private static String cutRedundantZeroAndDot(String s){
        if(s.indexOf(".") > 0){
            s = s.replaceAll("0+?$", "");//去掉多余的0
            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return s;
    }

    private String getMapJsonString(String building) {
        String querry = "select ROWID, Color, Floor, ASGeoJson(Geometry) from " + GeoTable;
        String mapString = null;
        try {
            Stmt stmt = database.prepare(querry);
            FeatureCollection fc = new FeatureCollection();
            try {
                while (stmt.step()) {
                    Feature feature = new Feature();
                    feature.setIdentifier(stmt.column_string(0));
                    JSONObject property = new JSONObject();
                    property.put("Color", stmt.column_long(1));
                    property.put("Floor", stmt.column_double(2));
                    feature.setProperties(property);
                    String geometryString = stmt.column_string(3);
                    Geometry geometry = generateGeometry(geometryString);
                    feature.setGeometry(geometry);
                    fc.addFeature(feature);
                }
                mapString = fc.toJSON().toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapString;
    }

    private String getPoiJsonString(String building) {
        String query = "select ROWID, Text, Floor, AsGeoJson(Geometry) from " + PoiTable;
        String poiString = null;
        try {
            Stmt stmt = database.prepare(query);
            FeatureCollection fc = new FeatureCollection();
            try {
                while (stmt.step()) {
                    Feature feature = new Feature();
                    feature.setIdentifier(stmt.column_string(0));
                    JSONObject property = new JSONObject();
                    property.put("Text", stmt.column_string(1));
                    property.put("Floor", stmt.column_double(2));
                    property.put("PicId", "pid140002");
                    feature.setProperties(property);
                    String geometryString = stmt.column_string(3);
                    Geometry geometry = generateGeometry(geometryString);
                    feature.setGeometry(geometry);
                    fc.addFeature(feature);
                }
                poiString = fc.toJSON().toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return poiString;
    }

    private List<Double> getFloorIds(String buildingId) {
        String query = "select DISTINCT Floor from " + GeoTable + " order by Floor";
        List<Double> floors = new ArrayList<>();
        try {
            Stmt stmt = database.prepare(query);
            while (stmt.step()) {
                floors.add(stmt.column_double(0));
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return floors;
    }

    public List<InterestPoint> searchPoi(String interestContent) {
        String query = "select ROWID, Text, ASGeoJson(Geometry) from " + PoiTable + " where Text Like '%" + interestContent + "%'";
        List<InterestPoint> pois = new ArrayList<>();
        try {
            Stmt stmt = database.prepare(query);
            while (stmt.step()) {
                InterestPoint poi = new InterestPoint();
                poi.id = stmt.column_long(0);
                poi.text = stmt.column_string(1);
                poi.imageId = 0;
                poi.geoJsonString = stmt.column_string(2);
                Point p = (Point) generateGeometry(poi.geoJsonString);
                poi.position = p.getPosition();
                pois.add(poi);
            }
            Timber.d("searchPoi, Find %d, for content %s", pois.size(), interestContent);
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pois;
    }

/*    public String searchRoute2(long poiFrom, long poiTo) {
        long routeNodeFrom = getRouteNode(poiFrom);
        long routeNodeTo = getRouteNode(poiTo);
        Timber.d("searchRoute2, Poi(%d -> %d), Translated to RoadNode(%d -> %d)", poiFrom, poiTo, routeNodeFrom, routeNodeTo);
        return searchRoute(routeNodeFrom, routeNodeTo);
    }*/

/*    private long getRouteNode(long poi) {
        double[] coordinate = getPoiCoordinate(poi);
        if (coordinate != null) {
            return getRouteNode("0", 2, coordinate);
        }
        return -1;
    }*/

    /*private double[] getPoiCoordinate(long poiId) {
        String query = "SELECT AsGeoJson(Geometry) from " + PoiTable + " where ROWID=" + poiId + " limit 0, 1";
        try {
            Stmt stmt = database.prepare(query);
            if (stmt.step()) {
                String geoJsonString = stmt.column_string(0);
                try {
                    JSONObject jo = new JSONObject(geoJsonString);
                    if ("Point".equals(jo.getString("type"))) {
                        JSONArray coordinates = jo.getJSONArray("coordinates");
                        double longitude = coordinates.getDouble(0);
                        double latitude = coordinates.getDouble(1);
                        return new double[]{longitude, latitude};
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/

/*    private long getRouteNode(String buildingId, double floorId, double[] coordinate) {
        String makepoint = makePointStatment(new Position(coordinate));
        String distance = "ST_Distance(Geometry, " + makepoint + ")";
        String minDistance = "Min(" + distance + ")";
        String query = "SELECT ROWID, " + minDistance + " from " + RoadNodeTable + " limit 0, 1";
        long routeNode = -1;
        try {
            Stmt stmt = database.prepare(query);
            if (stmt.step()) {
                routeNode = stmt.column_long(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routeNode;
    }*/

    /*private String searchRoute(long nodeFrom, long nodeTo) {
        Timber.d("searchRoute, node (%d -> %d)",nodeFrom, nodeTo);
        String query = "SELECT NodeFrom, NodeTo, Cost,  ASGeoJson(Geometry) from " + RoadSearchTable + " where NodeFrom = " + nodeFrom + " AND NodeTo = " + nodeTo + " limit 0, 1";
        String retPath = null;
        try {
            FeatureCollection fc = new FeatureCollection();
            Stmt stmt = database.prepare(query);
            if (stmt.step()) {
                Feature feature = new Feature();
                String geometryString = stmt.column_string(3);
                try {
                    Geometry geometry = generateGeometry(geometryString);
                    feature.setGeometry(geometry);
                    JSONObject property = new JSONObject();
                    property.put("NodeFrom", stmt.column_string(0));
                    property.put("NodeTo", stmt.column_string(1));
                    property.put("Cost", stmt.column_string(2));
                    feature.setProperties(property);
                    fc.addFeature(feature);
                    retPath = fc.toJSON().toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retPath;
    }*/

    public String searchPath(double[] from, double[] to) {
        Timber.d("searchPath: (%f, %f) -> (%f, %f)", from[0], from[1], to[0], to[1]);
        ClosestRoad f = searchClosestRoad(from);
        ClosestRoad t = searchClosestRoad(to);
        PathInfo p11 = correctPath(getPath(f.node1, t.node1), f, t);
        PathInfo p12 = correctPath(getPath(f.node1, t.node2), f, t);
        PathInfo p21 = correctPath(getPath(f.node2, t.node1), f, t);
        PathInfo p22 = correctPath(getPath(f.node2, t.node2), f, t);
        PathInfo minP = getMinPath(p11, p12);
        minP = getMinPath(minP, p21);
        minP = getMinPath(minP, p22);
        Timber.d("SearchPath: The minimum path: from, %d, to, %d, cost,%f", minP.from, minP.to, minP.cost);

        FeatureCollection fc = new FeatureCollection();
        Feature feature = new Feature();

        Geometry geometry = generateGeometry(minP.geoJsonString);
        feature.setGeometry(geometry);
        fc.addFeature(feature);

        String featureCollectionsString = null;
        try {
            featureCollectionsString = fc.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Timber.d("SearchPath: Feature collections String: %s", featureCollectionsString);
        return featureCollectionsString;
    }

    private ClosestRoad searchClosestRoad(double[] coordinate) {
        ClosestRoad clstRd = null;
        Position refPosition = new Position(coordinate);

        String refPoint = makePointStatment(refPosition);
        String distance = "ST_DISTANCE(Geometry, "+refPoint +")";
        String minDistance = "MIN(" + distance + ")";
        String closestPoint = "ClosestPoint(Geometry, " + refPoint + ")";
        String query = "SELECT ROWID,  PK_UID_1,  PK_UID_2,  AsGeoJson("+closestPoint+"), "  + minDistance + " from " + RoadTable;

        try {
            Stmt stmt = database.prepare(query);
            int i=0;
            if (stmt.step()) {
                clstRd = new ClosestRoad();
                clstRd.refPosition = refPosition;
                clstRd.roadId = stmt.column_long(0);
                clstRd.node1 = stmt.column_long(1);
                clstRd.node2 = stmt.column_long(2);
                clstRd.distance = stmt.column_double(4);
                String closetPointGeoJsonString = stmt.column_string(3);
                Point closetPoint = (Point) generateGeometry(closetPointGeoJsonString);
                clstRd.closestPoint = closetPoint.getPosition();
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clstRd;
    }

    private PathInfo getPath(long nodeFrom, long nodeTo) {
        String query = "SELECT ArcRowid, NodeFrom, NodeTo, Cost, AsGeoJson(Geometry) from " + RoadSearchTable + " where NodeFrom = " + nodeFrom + " AND NodeTo = " + nodeTo;
        PathInfo pathInfo = null;
        try {
            Stmt stmt = database.prepare(query);
            if (stmt.step()) {
                pathInfo = new PathInfo();
                pathInfo.from = stmt.column_long(1);
                pathInfo.to = stmt.column_long(2);
                pathInfo.cost = stmt.column_double(3);
                pathInfo.geoJsonString = stmt.column_string(4);
                while (stmt.step()) {
                    RoadInfo ri = new RoadInfo();
                    ri.roadId = stmt.column_long(0);
                    ri.from = stmt.column_long(1);
                    ri.to = stmt.column_long(2);
                    ri.cost = stmt.column_double(3);
                    pathInfo.roadInfos.add(ri);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pathInfo;
    }

    private PathInfo correctPath(PathInfo pi, ClosestRoad from, ClosestRoad to) {
        LineString ls = (LineString) generateGeometry(pi.geoJsonString);
        List<Position> ps = new ArrayList<>(ls.getPositions());
        RoadInfo firstRoad = pi.getFirstRoad();
        RoadInfo lastRoad = pi.getLastRoad();
        if (firstRoad.roadId == from.roadId) {
            ps.remove(0);
            pi.cost -= firstRoad.cost;
        }
        pi.cost += getDistance(from.closestPoint, ps.get(0));
        ps.add(0, from.closestPoint);

        if (lastRoad.roadId == to.roadId) {
            ps.remove(ps.size()-1);
            pi.cost -= lastRoad.cost;
        }
        pi.cost += getDistance(to.closestPoint, ps.get(ps.size()-1));
        ps.add(to.closestPoint);
        ls.setPositions(ps);
        try {
            pi.geoJsonString = ls.toJSON().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pi;
    }

    private PathInfo getMinPath(PathInfo p1, PathInfo p2) {
        if (p1.cost < p2.cost) {
            return p1;
        } else {
            return p2;
        }
    }

    private double getDistance(Position p1, Position p2) {
        double distance = -1;
        String point1 = makePointStatment(p1);
        String point2 = makePointStatment(p2);
        String query = "Select ST_Distance(" + point1 + ", " + point2 + ")";
        try {
            Stmt stmt = database.prepare(query);
            if (stmt.step()) {
                distance = stmt.column_double(0);
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return distance;
    }

    private class PathInfo {
        long from;
        long to;
        double cost;
        String geoJsonString;
        List<MapManager.RoadInfo> roadInfos = new ArrayList<>();

        MapManager.RoadInfo getFirstRoad() {
            return roadInfos.get(0);
        }

        MapManager.RoadInfo getLastRoad() {
            return roadInfos.get(roadInfos.size()-1);
        }
    }

    class RoadInfo {
        long roadId;
        long from;
        long to;
        double cost;
    }

    private class ClosestRoad {
        long roadId;
        long node1;
        long node2;

        Position refPosition;
        Position closestPoint;

        double distance; //Distance between refPosition and closestPoint
    }

    private Geometry generateGeometry(String geometryString) {
        Geometry geometry = null;
        try {
            JSONObject jo = new JSONObject(geometryString);
            String type = jo.get(Feature.JSON_TYPE).toString();
            switch(type) {
                case GeoJSON.TYPE_POINT:
                    geometry = new Point(jo);
                    break;
                case GeoJSON.TYPE_MULTI_POINT:
                    geometry = new MultiPoint(jo);
                    break;
                case GeoJSON.TYPE_LINE_STRING:
                    geometry = new LineString(jo);
                    break;
                case GeoJSON.TYPE_MULTI_LINE_STRING:
                    geometry = new MultiLineString(jo);
                    break;
                case GeoJSON.TYPE_POLYGON:
                    geometry = new Polygon(jo);
                    break;
                case GeoJSON.TYPE_MULTI_POLYGON:
                    geometry = new MultiPolygon(jo);
                    break;
                case GeoJSON.TYPE_GEOMETRY_COLLECTION:
                    geometry = new GeometryCollection(jo);
                    break;
                default:
                    Timber.d("Unknow geometry: %s", type);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return geometry;
    }

    private void spatialiteVersion() {
        if (database != null) {
            try {
                Stmt stmt = database.prepare("SELECT spatialite_version();");
                if (stmt.step()) {
                    Timber.d("spatialite_version: %s", stmt.column_string(0));
                    stmt.close();
                }
                stmt = database.prepare("SELECT proj4_version();");
                if (stmt.step()) {
                    Timber.d("proj4_version: %s", stmt.column_string(0));
                    stmt.close();
                }
                stmt = database.prepare("SELECT geos_version();");
                if (stmt.step()) {
                    Timber.d("geos_version: %s", stmt.column_string(0));
                    stmt.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String makePointStatment(Position position) {
        return "MakePointZ("+position.getLongitude()+", " + position.getLatitude() + ", " + position.getAltitude() + ")";
    }

    private void storeGeoJson(String json) {
        String dbDir = Environment.getExternalStorageDirectory() + File.separator + "IDMap" + File.separator;
        String storeName = "parseJson.json";
        String storePath = dbDir + File.separator + storeName;
        Timber.d(storePath);

        File jsonFile = new File(storePath);
        jsonFile.deleteOnExit();

        FileWriter fw;
        try {
            fw = new FileWriter(jsonFile);
            fw.write(json);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
