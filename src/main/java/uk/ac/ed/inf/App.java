package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import com.mapbox.geojson.*;
import com.mapbox.turf.*;
import java.util.*;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        String date = args[0];
        String baseUrlStr = args[1];
        if (!baseUrlStr.endsWith("/")) {
            baseUrlStr += "/";
        }
        try {
            Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrlStr));
            Order[] orders = Order.getOrders(baseUrlStr, date);
            Delivery[] deliveries = getDeliveries(orders, restaurants);
            /** do this later
             writeDeliveries(date, deliveries);
             */
            List<DroneMove> flightpath = new ArrayList<>();
            List<Point> coordinates = new ArrayList<Point>();
            LngLat position = new LngLat(-3.186874, 55.944494);
            int i = 0;
            MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr, date);
            for (Order order: orders) {
                List<DroneMove> orderPath = new ArrayList<>();
                if (order.getValidity(restaurants).equals("Valid")) {
                    List<LngLat> explored = new ArrayList<LngLat>();
                    Restaurant restaurant = getRestaurant(order, restaurants);
                    LngLat goal = new LngLat(restaurant.longitude, restaurant.latitude);
                    while (!position.closeTo(goal)) {
                        float bestMove = findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), bestMove, newPosition.longitude(), newPosition.latitude(), i + 1));
                        i++;
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        position = newPosition;
                    }
                    orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), i + 1));
                    goal = new LngLat(-3.186874, 55.944494);
                    explored = new ArrayList<LngLat>();
                    while (!position.closeTo(goal)) {
                        //System.out.println(position);
                        float bestMove = findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), bestMove, newPosition.longitude(), newPosition.latitude(), i + 1));
                        i++;
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        position = newPosition;
                    }
                    orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), i + 1));
                    if (orderPath.size() + flightpath.size() <= 2000){
                        for (DroneMove move: orderPath){
                            flightpath.add(move);
                        }
                        for (Delivery delivery: deliveries){
                            if (delivery.orderNo == order.orderNo){
                                delivery.outcome = OrderOutcome.Delivered.toString();
                            }
                        }
                    }
                    else {
                        for (Delivery delivery: deliveries){
                            if (delivery.orderNo == order.orderNo){
                                delivery.outcome = OrderOutcome.ValidButNotDelivered.toString();
                            }
                        }
                    }
                }
            }
            LineString lineString = LineString.fromLngLats(coordinates);
            String json = FeatureCollection.fromFeature(Feature.fromGeometry((Geometry) lineString)).toJson();
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
            Feature path = Feature.fromGeometry((Geometry) lineString);
            Feature noflypolygon = Feature.fromGeometry((MultiPolygon) noFlyZone);
            List<Feature> features = new ArrayList<Feature>();
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

    public static Delivery[] getDeliveries(Order[] orders, Restaurant[] restaurants){
        Delivery[] deliveries = new Delivery[orders.length];
        for (int i = 0; i < orders.length; i++) {
            deliveries[i] = new Delivery(orders[i].orderNo, orders[i].getValidity(restaurants), orders[i].priceTotalInPence);
        }
        return deliveries;
    }

    public static void writeDeliveries(String date, Delivery[] deliveries){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/deliveries-" + date + ".json").toFile(), deliveries);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float findBestMove(LngLat position, LngLat goal, MultiPolygon noFlyZone, List<LngLat> explored){
        float bestMove = -1;
        double bestDistance = 1000;
        for (float i = 0; i < 360; i += 22.5){
            boolean visited = false;
            for (LngLat point: explored){
                if (position.nextPosition(i).closeTo(point)){
                    visited = true;
                    break;
                }
            }
            if (!visited) {
                boolean intersects = false;
                for (List<List<Point>> polygon: noFlyZone.coordinates()){
                    for (int j = 0; j < polygon.get(0).size()-1; j++){
                        List<Point> border = new ArrayList<Point>();
                        border.add(polygon.get(0).get(j));
                        border.add(polygon.get(0).get(j+1));
                        LineString borderline = LineString.fromLngLats(border);
                        List<Point> path = new ArrayList<Point>();
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
                if (!intersects) {
                    if (position.nextPosition(i).distanceTo(goal) < bestDistance) {
                        bestMove = i;
                        bestDistance = position.nextPosition(i).distanceTo(goal);
                    }
                }
            }
        }
        return bestMove;
    }

    public static Restaurant getRestaurant(Order order, Restaurant[] restaurants){
        for (Restaurant restaurant: restaurants){
            for (Menu item: restaurant.getMenu()){
                if (item.name.equals(order.orderItems[0])){
                    return restaurant;
                }
            }
        }
        return null;
    }

    public static boolean linesIntersect(LineString line1, LineString line2){
        double latstart1 = line1.coordinates().get(0).latitude();
        double latend1 = line1.coordinates().get(1).latitude();
        double longstart1 = line1.coordinates().get(0).longitude();
        double longend1 = line1.coordinates().get(1).longitude();
        double latstart2 = line2.coordinates().get(0).latitude();
        double latend2 = line2.coordinates().get(1).latitude();
        double longstart2 = line2.coordinates().get(0).longitude();
        double longend2 = line2.coordinates().get(1).longitude();

        double denominator = ((longend2 - longstart2) * (latend1 - latstart1)) - ((latend2 - latstart2) * (longend1 - longstart1));
        if (denominator == 0){
            return false;
        }
        double a = longstart1 - longstart2;
        double b = latstart1 - latstart2;

        double numerator1 = ((latend2 - latstart2) * a) - ((longend2 - longstart2) * b);
        double numerator2 = ((latend1 - latstart1) * a) - ((longend1 - longstart1) * b);
        a = numerator1 / denominator;
        b = numerator2 / denominator;

        return ((a > 0 && a < 1) && (b > 0 && b < 1));
    }
}