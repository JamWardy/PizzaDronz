package uk.ac.ed.inf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbox.geojson.*;

import java.io.IOException;
import java.lang.Math;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a point as a latitude and longitude pair of co-ordinates.
 * @param longitude The longitude of the point.
 * @param latitude The latitude of the point.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LngLat(double longitude, double latitude){
    /**
     * Returns whether the point represented by this LngLat object is within the central area, which is represented by the CentralArea class.
     * @return Boolean value for whether the point defined by this LngLat object is within the central area.
     * @param url The URL that the central area co-ordinates are accessed from
     */
    public boolean inCentralArea(URL url){
        CentralArea centralArea = CentralArea.getInstance(url);
        if (this.longitude <= centralArea.getPoints()[0].longitude || this.latitude >= centralArea.getPoints()[0].latitude){
            return false;
        }
        if (this.longitude <= centralArea.getPoints()[1].longitude || this.latitude <= centralArea.getPoints()[1].latitude){
            return false;
        }
        if (this.longitude >= centralArea.getPoints()[2].longitude || this.latitude <= centralArea.getPoints()[2].latitude){
            return false;
        }
        if (this.longitude >= centralArea.getPoints()[3].longitude || this.latitude >= centralArea.getPoints()[3].latitude){
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * Default method for when no URL is passed as a parameter, calls inCentralArea(url) with a default url value.
     * @return Boolean value for whether the point defined by this LngLat object is within the central area.
     */
    public boolean inCentralArea() {
        try {
            URL url = new URL("https://ilp-rest.azurewebsites.net/centralArea");
            return inCentralArea(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the distance in degrees between this point and another point.
     * @param point The point to which the distance is being calculated.
     * @return Euclidean distance between this point and the point inputted as a parameter as a double.
     */
    public double distanceTo(LngLat point){
        return Math.sqrt(Math.pow(point.longitude - this.longitude,2) + Math.pow(point.latitude - this.latitude,2));
    }

    /**
     * Returns whether the distance between this and another point is strictly less than 0.00015 degrees.
     * @param point The point which we are seeing if this point is close to.
     * @return A boolean for if this point is close to the point inputted as a parameter.
     */
    public boolean closeTo(LngLat point){
        return (this.distanceTo(point) < 0.00015);
    }

    /**
     * Returns a LngLat object for the position of the drone after a move of an inputted number of degrees.
     * @param move The angle in degrees at which the drone moves.
     * @return A LngLat object with the new position of the drone.
     */
    public LngLat nextPosition(double move){
        if (move == -1){
            return (new LngLat(this.longitude, this.latitude));
        }
        else {
            double newLatitude = this.latitude + Math.cos(Math.toRadians(move)) * 0.00015;
            double newLongitude = this.longitude + Math.sin(Math.toRadians(move)) * 0.00015;
            LngLat newPosition = new LngLat(newLongitude, newLatitude);
            return newPosition;
        }
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
                for (double[] point: zone.coordinates) {
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

    /**
     * Finds the best legal unexplored drone move at a given position, trying to get to a given goal.
     * @param position  Current position of the drone, as a LngLat.
     * @param goal      Goal the drone is trying to get to, as a LngLat.
     * @param noFlyZone No-Fly zones that can't be flown through, as a mapbox MultiPolygon object.
     * @param explored  List of LngLat points already explored by the drone on the path to the goal.
     * @param centralURL    URL of the Central Area
     * @return  A float for the best move.
     */
    public static float findBestMove(LngLat position, LngLat goal, MultiPolygon noFlyZone, List<LngLat> explored,  URL centralURL){
        float bestMove = -1;
        double bestDistance = 1000;
        // search through all possible moves
        for (float i = 0; i < 360; i += 22.5){
            boolean visited = false;
            // check point has not already been explored on the path
            for (LngLat point: explored){
                // if possible move is close to a point that has already been explored, mark as visited
                if (position.nextPosition(i).closeTo(point)){
                    visited = true;
                    break;
                }
            }
            if (!visited) {
                // check if the move would cause the flightpath to go through the no-fly zone
                boolean intersects = intersectsNoFlyZone(noFlyZone, position, i);
                if (!intersects) {
                    // if the move would be an improvement compared to the current best move (smaller euclidean distance to the goal)
                    if (position.nextPosition(i).distanceTo(goal) < bestDistance) {
                            // if the move has left central area it cannot return
                            if (position.inCentralArea(centralURL) || !position.nextPosition(i).inCentralArea(centralURL)) {
                                // select the current move as a new best
                                bestMove = i;
                                bestDistance = position.nextPosition(i).distanceTo(goal);
                            }
                    }
                }
            }
        }
        return bestMove;
    }
}
