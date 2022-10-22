package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import com.mapbox.geojson.*;
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
            Delivery[] deliveries = new Delivery[orders.length];
            for (int i = 0; i < orders.length; i++) {
                deliveries[i] = new Delivery(orders[i].orderNo, orders[i].getValidity(restaurants), orders[i].priceTotalInPence);
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/deliveries-" + date + ".json").toFile(), deliveries);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DroneMove[] flightpath = new DroneMove[2000];
        List<Point> coordinates = new ArrayList<Point>();
        LngLat position = new LngLat(-3.186874, 55.944494);
        for (int i = 0; i < 100; i++){
            LngLat newPosition = position.nextPosition(0);
            flightpath[i] = new DroneMove(null, position.longitude(), position.latitude(), 0, newPosition.longitude(), newPosition.latitude(), i+1);
            coordinates.add(Point.fromLngLat(position.longitude(), position.latitude()));
            position = newPosition;
        }
        LineString lineString = LineString.fromLngLats(coordinates);
        String json = FeatureCollection.fromFeature(Feature.fromGeometry((Geometry) lineString)).toJson();
        try{
            FileWriter filewriter = new FileWriter("resultfiles/drone-" + date + ".geojson");
            filewriter.write(json);
            filewriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}