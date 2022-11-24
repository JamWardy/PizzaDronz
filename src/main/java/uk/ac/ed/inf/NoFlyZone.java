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
 * Representation of a No-Fly Zone, in the format returned by the REST-server.
 */
public class NoFlyZone {
    private String name;
    private double[][] coordinates;

    public NoFlyZone(){};

    public String getName() {
        return name;
    }

    /**
     * @return  List of co-ordinate pairs that define the vertices of the No-Fly Zone polygon in the format required for a mapbox.geojson Polygon.
     */
    public double[][] getCoordinates() {
        return coordinates;
    }

    /**
     * Retrieves the No-Fly Zones from the REST server.
     * @param baseUrlStr    The base of the URL from which the No-Fly Zones are retrieved.
     * @return  A MultiPolygon object of the No-Fly Zones.
     */
    public static MultiPolygon getNoFlyZones(String baseUrlStr){
        try{
            URL noflyurl = new URL(baseUrlStr + "noFlyZones");
            // retrieve array of NoFlyZone objects from REST server
            NoFlyZone[] noFlyZone = new ObjectMapper().readValue(noflyurl, NoFlyZone[].class);
            // list of mapbox.geojson Polygon objects, the format required to make a mapbox.geojson MultiPolygon object
            List<Polygon> polygons = new ArrayList<>(){};
            for (NoFlyZone zone: noFlyZone){
                // create 2d list of points, the format required to make a mapbox.geojson Polygon object
                List<List<Point>> points = new ArrayList<>();
                // initialise first list in points, which holds the vertices that define polygon
                points.add(new ArrayList<>());
                for (double[] point: zone.getCoordinates()) {
                    // add each point in the no-fly zone to the formatted list of points
                    points.get(0).add(Point.fromLngLat(point[0], point[1]));
                }
                // create a Polygon object from the 2d list of points
                polygons.add(Polygon.fromLngLats(points));
            }
            // create a MultiPolygon object from the list of Polygon objects
            return MultiPolygon.fromPolygons(polygons);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a drone move intersects the No-Fly zones.
     * @param noFlyZones The No-Fly Zones of the area, as a mapbox MultiPolygon object.
     * @param position  The current position of the drone, as a LngLat object.
     * @param i         The angle of the drone move.
     * @return          A boolean for whether the drone move intersects the No-Fly Zone.
     */
    public static boolean intersectsNoFlyZones(MultiPolygon noFlyZones, LngLat position, float i){
        boolean intersects = false;
        // check intersections for each no-fly zone in the no-fly zones
        for (List<List<Point>> polygon: noFlyZones.coordinates()){
            // iterate through each vertex on the no-fly zone (includes the first vertex twice)
            for (int j = 0; j < polygon.get(0).size()-1; j++){
                // add the vertex and its adjacent vertex to a list
                List<Point> border = new ArrayList<>();
                border.add(polygon.get(0).get(j));
                border.add(polygon.get(0).get(j+1));
                // create a line segment from the 2 points
                LineString borderline = LineString.fromLngLats(border);
                // create a line segment from the 2 positions of the drone before and after the move considered
                List<Point> path = new ArrayList<>();
                path.add(Point.fromLngLat(position.longitude(), position.latitude()));
                path.add(Point.fromLngLat(position.nextPosition(i).longitude(), position.nextPosition(i).latitude()));
                LineString pathline = LineString.fromLngLats(path);
                // if the two lines intersect, the drone intersects the no-fly zone and we no longer need to consider any more lines
                if (linesIntersect(borderline, pathline)){
                    intersects = true;
                    break;
                }
            }
            // if an intersection has been found, there is no need to consider anymore lines
            if (intersects){
                break;
            }
        }
        // return whether a line intersection has been found
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

        // (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        double denominator = ((latstart1 - latend1) * (longstart2 - longend2)) - ((longstart1 - longend1) * (latstart2 - latend2));
        // if the denominator is zero the 2 lines are parallel and so never intersect
        if (denominator == 0){
            return false;
        }

        // (x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)
        double t = ((latstart1 - latstart2) * (longstart2 - longend2) - (longstart1 - longstart2) * (latstart2 - latend2)) / denominator;

        // (x1 - x3) * (y1 - y2) - (y1 - y3) * (x1 - x2)
        double u = ((latstart1 - latstart2) * (longstart1 - longend1) - (longstart1 - longstart2) * (latstart1 - latend1)) / denominator;

        // lines intersect only if t and u are between 0 and 1 inclusive
        return ((t >= 0 && t <= 1) && (u >= 0 && u <= 1));
    }
}
