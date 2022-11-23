package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a No-Fly Zone, in the format returned by the REST-server
 */
public class NoFlyZone {
    private String name;
    private double[][] coordinates;

    public NoFlyZone(){};

    public String getName() {
        return name;
    }

    public double[][] getCoordinates() {
        return coordinates;
    }

    /**
     * Retrives the No-Fly Zone from the REST server.
     * @param baseUrlStr    The base of the URL from which the No-Fly Zone is retrieved.
     * @return  A MultiPolygon object of the No-Fly Zone.
     */
    public static MultiPolygon getNoFlyZone(String baseUrlStr){
        try{
            URL noflyurl = new URL(baseUrlStr + "noFlyZones");
            NoFlyZone[] noFlyZone = new ObjectMapper().readValue(noflyurl, NoFlyZone[].class);
            List<Polygon> polygons = new ArrayList<Polygon>(){};
            for (NoFlyZone zone: noFlyZone){
                List<List<Point>> points = new ArrayList<>();
                points.add(new ArrayList<>());
                for (double[] point: zone.getCoordinates()) {
                    points.get(0).add(Point.fromLngLat(point[0], point[1]));
                }
                polygons.add(Polygon.fromLngLats(points));
            }
            return MultiPolygon.fromPolygons(polygons);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a drone move intersects the No-Fly zone.
     * @param noFlyZone The No-Fly Zone of the area, as a mapbox MultiPolygon object.
     * @param position  The current position of the drone, as a LngLat object.
     * @param i         The angle of the drone move.
     * @return          A boolean for whether the drone move intersects the No-Fly Zone.
     */
    public static boolean intersectsNoFlyZone(MultiPolygon noFlyZone, LngLat position, float i){
        boolean intersects = false;
        for (List<List<Point>> polygon: noFlyZone.coordinates()){
            for (int j = 0; j < polygon.get(0).size()-1; j++){
                List<Point> border = new ArrayList<Point>();
                border.add(polygon.get(0).get(j));
                border.add(polygon.get(0).get(j+1));
                LineString borderline = LineString.fromLngLats(border);
                List<Point> path = new ArrayList<>();
                path.add(Point.fromLngLat(position.longitude(), position.latitude()));
                path.add(Point.fromLngLat(position.nextPosition(i).longitude(), position.nextPosition(i).latitude()));
                LineString pathline = LineString.fromLngLats(path);
                if (linesIntersect(borderline, pathline)){
                    intersects = true;
                    break;
                }
            }
            if (intersects){
                break;
            }
        }
        return intersects;
    }

    /**
     * Check if two line segments intersect. Algorithm based on solution to the line-line intersection problem found at
     * https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection#Given_two_points_on_each_line_segment
     * which solves the problem in terms of Bezier parameters, t and u.
     * @param line1 First line, a LineString.
     * @param line2 Second line, a LineString.
     * @return  A boolean for whether the two lines segments intersect eachother.
     */
    public static boolean linesIntersect(LineString line1, LineString line2){
        double latstart1 = line1.coordinates().get(0).latitude(); //x1
        double latend1 = line1.coordinates().get(1).latitude(); //x2
        double longstart1 = line1.coordinates().get(0).longitude(); //y1
        double longend1 = line1.coordinates().get(1).longitude(); //y2
        double latstart2 = line2.coordinates().get(0).latitude(); //x3
        double latend2 = line2.coordinates().get(1).latitude(); //x4
        double longstart2 = line2.coordinates().get(0).longitude(); //y3
        double longend2 = line2.coordinates().get(1).longitude(); //y4

        // (x1-x2) * (y3-y4) - (y1 - y2) * (x3 - x4)
        double denominator = ((latstart1 - latend1) * (longstart2 - longend2)) - ((longstart1 - longend1) * (latstart2 - latend2));
        if (denominator == 0){
            return false;
        }

        //(x1-x3)(y3-y4) - (y1-y3)(x3-x4)
        double t = ((latstart1 - latstart2) * (longstart2 - longend2) - (longstart1 - longstart2) * (latstart2 - latend2)) / denominator;

        // (x1-x3)(y1-y2) - (y1-y3)(x1-x2)
        double u = ((latstart1 - latstart2) * (longstart1 - longend1) - (longstart1 - longstart2) * (latstart1 - latend1)) / denominator;

        return ((t > 0 && t < 1) && (u > 0 && u < 1));
    }
}
