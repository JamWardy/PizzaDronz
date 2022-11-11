package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import com.mapbox.geojson.*;
import java.util.*;

public class App {

    /**
     * Invokes the program
     * @param args date (the date for which the orders are processed) baseUrlString (the URL base to which REST-requests are made).
     */
    public static void main(String[] args) {
        String date = args[0];
        String baseUrlStr = args[1];
        if (!baseUrlStr.endsWith("/")) {
            baseUrlStr += "/";
        }
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrlStr));
            Order[] orders = Order.getOrders(baseUrlStr, date);
            //Order[] orders = Order.getOrdersNoDate(baseUrlStr);
            Delivery[] deliveries = Delivery.getDeliveries(orders, restaurants);
            List<DroneMove> flightpath = new ArrayList<>();
            LngLat position = new LngLat(-3.186874, 55.944494);
            int i = 0;
            MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr, date);
            for (Order order: orders) {
                List<DroneMove> orderPath = new ArrayList<>();
                if (order.getValidity(restaurants).equals("Valid")) {
                    Restaurant restaurant = Order.getRestaurant(order, restaurants);
                    orderPath.addAll(orderPathToGoal(position, new LngLat(restaurant.longitude, restaurant.latitude), noFlyZone, order, i));
                    position = new LngLat(orderPath.get(orderPath.size() - 1).toLongitude, orderPath.get(orderPath.size() - 1).toLatitude);
                    i = orderPath.get(orderPath.size() - 1).ticksSinceStartOfCalculation;
                    orderPath.addAll(orderPathToGoal(position, new LngLat(-3.186874, 55.944494), noFlyZone, order, i));
                    position = new LngLat(orderPath.get(orderPath.size() - 1).toLongitude, orderPath.get(orderPath.size() - 1).toLatitude);
                    i = orderPath.get(orderPath.size() - 1).ticksSinceStartOfCalculation;
                    if (orderPath.size() + flightpath.size() <= 2000){
                        flightpath.addAll(orderPath);
                        deliveries = Delivery.setDelivered(deliveries, order);
                    }
                    else {
                        deliveries = Delivery.setValidNotDelivered(deliveries, order);
                    }
                }
            }
            List<Point> coordinates = new ArrayList<>();
            for (DroneMove move: flightpath){
                coordinates.add(Point.fromLngLat(move.fromLongitude, move.fromLatitude));
            }
            coordinates.add(Point.fromLngLat(flightpath.get(flightpath.size()-1).fromLongitude, flightpath.get(flightpath.size()-1).fromLatitude));
            LineString lineString = LineString.fromLngLats(coordinates);
            String json = FeatureCollection.fromFeature(Feature.fromGeometry(lineString)).toJson();
            writeGeojson(json, date);
            writeDeliveries(date, deliveries);
            writeFlightPath(flightpath, date);
            //writeCombined(lineString, noFlyZone, baseUrlStr, date);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes to a geoJson file which contains the drone flightpath as well as the No-Fly Zones and the central area,
     * at 'resultfiles/combined-date.geojson'.
     * @param flightpath    The flight path of the drone as a LineString.
     * @param noFlyZone     The No-Fly Zone as a Mapbox MultiPolygon.
     * @param baseUrlStr    The base URL from which the central area's co-ordinates are requested.
     * @param date          The date for which the flight path is generated
     */
    public static void writeCombined(LineString flightpath, MultiPolygon noFlyZone, String baseUrlStr, String date){
        try {
            Feature path = Feature.fromGeometry(flightpath);
            Feature noflypolygon = Feature.fromGeometry(noFlyZone);
            List<Feature> features = new ArrayList<>();
            features.add(path);
            features.add(noflypolygon);
            List<List<Point>> centralAreaPoints = new ArrayList<>();
            centralAreaPoints.add(new ArrayList<>());
            LngLat[] centralArea = CentralArea.getInstance(new URL(baseUrlStr + "centralArea")).points;
            for (LngLat point : centralArea) {
                centralAreaPoints.get(0).add(Point.fromLngLat(point.longitude(), point.latitude()));
            }
            centralAreaPoints.get(0).add(Point.fromLngLat(centralArea[0].longitude(), centralArea[0].latitude()));
            features.add(Feature.fromGeometry(Polygon.fromLngLats(centralAreaPoints)));
            String combinedJson = FeatureCollection.fromFeatures(features).toJson();
            FileWriter combinedWriter = new FileWriter("resultfiles/combined-" + date + ".geojson");
            combinedWriter.write(combinedJson);
            combinedWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<DroneMove> orderPathToGoal(LngLat position, LngLat goal, MultiPolygon noFlyZone, Order order, int i){
        List<DroneMove> orderPath = new ArrayList<>();
        List<LngLat> explored = new ArrayList<>();
        while (!position.closeTo(goal)) {
            float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored);
            LngLat newPosition = position.nextPosition(bestMove);
            orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), ++i ));
            explored.add(position);
            position = newPosition;
        }
        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), ++i));
        return orderPath;
    }


    public static void writeFlightPath(List<DroneMove> flightpath, String date){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/flightpath-" + date + ".json").toFile(), flightpath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeGeojson(String json, String date){
        try{
            FileWriter filewriter = new FileWriter("resultfiles/drone-" + date + ".geojson");
            filewriter.write(json);
            filewriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeDeliveries(String date, Delivery[] deliveries){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/deliveries-" + date + ".json").toFile(), deliveries);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}