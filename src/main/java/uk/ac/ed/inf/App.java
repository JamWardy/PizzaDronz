package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import com.mapbox.geojson.*;
import java.util.*;

public class App {
    public static void main(String[] args) {
        String date = args[0];
        String baseUrlStr = args[1];
        if (!baseUrlStr.endsWith("/")) {
            baseUrlStr += "/";
        }
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrlStr));
            //Order[] orders = Order.getOrders(baseUrlStr, date);
            Order[] orders = Order.getOrdersNoDate(baseUrlStr);
            Delivery[] deliveries = Delivery.getDeliveries(orders, restaurants);
            List<DroneMove> flightpath = new ArrayList<>();
            List<Point> coordinates = new ArrayList<>();
            LngLat position = new LngLat(-3.186874, 55.944494);
            int i = 0;
            MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr, date);
            for (Order order: orders) {
                List<DroneMove> orderPath = new ArrayList<>();
                if (order.getValidity(restaurants).equals("Valid")) {
                    List<LngLat> explored = new ArrayList<LngLat>();
                    Restaurant restaurant = Order.getRestaurant(order, restaurants);
                    LngLat goal = new LngLat(restaurant.longitude, restaurant.latitude);
                    //orderPath.addAll(getPathCoordinates(position, goal, noFlyZone, order, i));
                    while (!position.closeTo(goal)) {
                        float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), ++i ));
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        position = newPosition;
                    }
                    orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), ++i));
                    goal = new LngLat(-3.186874, 55.944494);
                    explored = new ArrayList<>();
                    while (!position.closeTo(goal)) {
                        //System.out.println(position);
                        float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), ++i ));
                        i++;
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        position = newPosition;
                    }
                    orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), ++i));
                    if (orderPath.size() + flightpath.size() <= 2000){
                        flightpath.addAll(orderPath);
                        for (Delivery delivery: deliveries){
                            if (delivery.orderNo.equals(order.orderNo)){
                                delivery.outcome = OrderOutcome.Delivered.toString();
                            }
                        }
                    }
                    else {
                        for (Delivery delivery: deliveries){
                            if (delivery.orderNo.equals(order.orderNo)){
                                delivery.outcome = OrderOutcome.ValidButNotDelivered.toString();
                            }
                        }
                    }
                }
            }
            LineString lineString = LineString.fromLngLats(coordinates);
            String json = FeatureCollection.fromFeature(Feature.fromGeometry(lineString)).toJson();
            writeResultFile(json, date);
            writeDeliveries(date, deliveries);
            writeFlightPath(flightpath, date);
            //writeCombined(lineString, noFlyZone, baseUrlStr, date);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeCombined(LineString lineString, MultiPolygon noFlyZone, String baseUrlStr, String date){
        try {
            Feature path = Feature.fromGeometry(lineString);
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

    public static void writeFlightPath(List<DroneMove> flightpath, String date){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/flightpath-" + date + ".json").toFile(), flightpath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeResultFile(String json, String date){
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

    public static List<DroneMove> getPathCoordinates(LngLat position, LngLat goal, MultiPolygon noFlyZone, Order order, int i){
        List<LngLat> explored = new ArrayList<>();
        List<DroneMove> orderPath = new ArrayList<>();
        while (!position.closeTo(goal)) {
            float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored);
            LngLat newPosition = position.nextPosition(bestMove);
            orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), i + 1));
            explored.add(position);
            position = newPosition;
        }
        return orderPath;
    }
}