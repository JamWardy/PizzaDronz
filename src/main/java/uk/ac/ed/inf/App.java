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
            DroneMove[] flightpath = new DroneMove[5000];
            List<Point> coordinates = new ArrayList<Point>();
            LngLat position = new LngLat(-3.186874, 55.944494);
            //for (Delivery delivery: deliveries) {
                /*LngLat newPosition = position.nextPosition(i);
                flightpath[i] = new DroneMove(null, position.longitude(), position.latitude(), 0, newPosition.longitude(), newPosition.latitude(), i + 1);
                coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                position = newPosition;
                 */
            //}
            int i = 0;
            MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr, date);
            for (Order order: orders) {
                if (order.getValidity(restaurants).equals("Valid")) {
                    List<LngLat> explored = new ArrayList<LngLat>();
                    Restaurant restaurant = getRestaurant(order, restaurants);
                    LngLat goal = new LngLat(restaurant.longitude, restaurant.latitude);
                    while (!position.closeTo(goal)) {
                        float bestMove = findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        flightpath[i] = new DroneMove(null, position.longitude(), position.latitude(), bestMove, newPosition.longitude(), newPosition.latitude(), i + 1);
                        i++;
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        //System.out.println(bestMove);
                        position = newPosition;
                    }
                    goal = new LngLat(-3.186874, 55.944494);
                    explored = new ArrayList<LngLat>();
                    while (!position.closeTo(goal)) {
                        //System.out.println(position);
                        float bestMove = findBestMove(position, goal, noFlyZone, explored);
                        LngLat newPosition = position.nextPosition(bestMove);
                        flightpath[i] = new DroneMove(null, position.longitude(), position.latitude(), bestMove, newPosition.longitude(), newPosition.latitude(), i + 1);
                        i++;
                        coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
                        explored.add(position);
                        position = newPosition;
                    }
                }
            }
            LineString lineString = LineString.fromLngLats(coordinates);
            String json = FeatureCollection.fromFeature(Feature.fromGeometry((Geometry) lineString)).toJson();
            writeResultFile(json, date);
            writeDeliveries(date, deliveries);
            Feature path = Feature.fromGeometry((Geometry) lineString);
            Feature noflypolygon = Feature.fromGeometry((MultiPolygon) noFlyZone);
            List<Feature> features = new ArrayList<Feature>();
            features.add(path);
            features.add(noflypolygon);
            String combinedJson = FeatureCollection.fromFeatures(features).toJson();
            FileWriter combinedWriter = new FileWriter("resultfiles/combined-" + date + ".geojson");
            combinedWriter.write(combinedJson);
            combinedWriter.close();
        }
        catch (IOException e) {
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
        for (float i = (float) 22.5; i < 360; i += 22.5){
            boolean visited = false;
            for (LngLat point: explored){
                if (position.nextPosition(i).closeTo(point)){
                    visited = true;
                    break;
                }
            }
            if (!visited) {
                if (!TurfJoins.inside(Point.fromLngLat(position.nextPosition(i).longitude(), position.nextPosition(i).latitude()), noFlyZone)) {
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
}