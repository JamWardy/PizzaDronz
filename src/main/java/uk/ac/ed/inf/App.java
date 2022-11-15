package uk.ac.ed.inf;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import com.mapbox.geojson.*;

import java.time.Clock;
import java.util.*;

public class App {
    int startTicks;

    /**
     * Invokes the program
     * @param args date (the date for which the orders are processed) baseUrlString (the URL base to which REST-requests are made).
     */
    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Invalid arguments, please input date and then url");
        }
        else {
            String date = args[0];
            String baseUrlStr = args[1];
            if (!baseUrlStr.endsWith("/")) {
                baseUrlStr += "/";
            }
            if (checkURLS(baseUrlStr, date)) {
                try {
                    Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrlStr));
                    Order[] orders = Order.getOrders(baseUrlStr, date);
                    //Order[] orders = Order.getOrdersNoDate(baseUrlStr);
                    if (orders.length > 0) {
                        Delivery[] deliveries = Delivery.getDeliveries(orders, restaurants);
                        List<DroneMove> flightpath = new ArrayList<>();
                        LngLat position = new LngLat(-3.186874, 55.944494);
                        MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr, date);
                        ArrayList<String[]> validOrders = new ArrayList<>();
                        for (Order order: orders){
                            double dist = position.distanceTo(new LngLat(order.getRestaurant(restaurants).longitude,order.getRestaurant(restaurants).latitude));
                            String[] orderInfo = {order.orderNo, Double.toString(dist)};
                            validOrders.add(orderInfo);
                        }
                        validOrders.sort(Comparator.comparingDouble(o -> Double.parseDouble(o[1])));
                        long startTicks = System.currentTimeMillis();
                        Order order = orders[0];
                        for (String[] orderitem : validOrders) {
                            for (Order temporder: orders){
                                if (temporder.orderNo.equals(orderitem[0])){
                                    order = temporder;
                                }
                            }
                            List<DroneMove> orderPath = new ArrayList<>();
                            if (order.getValidity(restaurants).equals("Valid")) {
                                Restaurant restaurant = order.getRestaurant(restaurants);
                                orderPath.addAll(orderPathToGoal(position, new LngLat(restaurant.longitude, restaurant.latitude), noFlyZone, order, startTicks));
                                position = new LngLat(orderPath.get(orderPath.size() - 1).toLongitude, orderPath.get(orderPath.size() - 1).toLatitude);
                                orderPath.addAll(orderPathToGoal(position, new LngLat(-3.186874, 55.944494), noFlyZone, order, startTicks));
                                position = new LngLat(orderPath.get(orderPath.size() - 1).toLongitude, orderPath.get(orderPath.size() - 1).toLatitude);
                                if (orderPath.size() + flightpath.size() <= 2000) {
                                    flightpath.addAll(orderPath);
                                    deliveries = Delivery.setDelivered(deliveries, order);
                                } else {
                                    deliveries = Delivery.setValidNotDelivered(deliveries, order);
                                }
                            }
                        }
                        System.out.println(System.currentTimeMillis() - startTicks);
                        List<Point> coordinates = new ArrayList<>();
                        for (DroneMove move : flightpath) {
                            coordinates.add(Point.fromLngLat(move.fromLongitude, move.fromLatitude));
                        }
                        coordinates.add(Point.fromLngLat(flightpath.get(flightpath.size() - 1).fromLongitude, flightpath.get(flightpath.size() - 1).fromLatitude));
                        writeGeojson(FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordinates))).toJson(), date);
                        writeDeliveries(date, deliveries);
                        writeFlightPath(flightpath, date);
                        //writeCombined(lineString, noFlyZone, baseUrlStr, date);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean checkURLS(String baseUrlStr, String date){
        boolean ok = false;
        try{
            URL baseUrl = new URL(baseUrlStr);
            URL orderUrl = new URL(baseUrl + "orders/" + date);
            URL centralAreaURL = new URL(baseUrl + "centralArea/");
            URL noFlyUrl = new URL(baseUrl + "noFlyZones/");
            URL restaurantsUrl = new URL(baseUrl + "restaurants/");
            new ObjectMapper().readValue(orderUrl, Order[].class);
            new ObjectMapper().readValue(centralAreaURL, LngLat[].class);
            new ObjectMapper().readValue(noFlyUrl, NoFlyZone[].class);
            new ObjectMapper().readValue(restaurantsUrl, Restaurant[].class);
            ok = true;
        }
        catch (MalformedURLException e){
            System.err.println("Malformed URL, please check URL protocol");
        }
        catch(IOException e){
            System.err.println("I/O Exception, please check connection and input data");
        }
        return ok;
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

    /**
     * Generates the list of drone moves for part of the drone's flightpath from the starting position to the goal.
     * @param position  Starting point of the drone as a LngLat point.
     * @param goal      Finishing point of the drone's flightpath as a LngLat point.
     * @param noFlyZone The No-Fly zones of the central area, which the drone should avoid flying through, as a Mapbox Multipolygon object.
     * @param order     The order which the drone is attempting to deliver.
     * @param startTicks         The initial number of ticks of the first calculation.
     * @return          A list of drone moves generated for this part of the flightpath.
     */
    public static List<DroneMove> orderPathToGoal(LngLat position, LngLat goal, MultiPolygon noFlyZone, Order order, long startTicks){
        List<DroneMove> orderPath = new ArrayList<>();
        List<LngLat> explored = new ArrayList<>();
        while (!position.closeTo(goal)) {
            float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored);
            LngLat newPosition = position.nextPosition(bestMove);
            orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), System.currentTimeMillis() - startTicks));
            explored.add(position);
            position = newPosition;
        }
        orderPath.add(new DroneMove(order.orderNo, position.longitude(), position.latitude() , position.longitude(), position.latitude(), System.currentTimeMillis() - startTicks));
        return orderPath;
    }

    /**
     * Writes the flightpath in terms of moves of the drone to the 'resultfiles/flightpath-date.json' file.
     * @param flightpath    The flightpath which will be written in the file.
     * @param date          The date of the orders for that flightpath, will be used as the file's name.
     */
    public static void writeFlightPath(List<DroneMove> flightpath, String date){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/flightpath-" + date + ".json").toFile(), flightpath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the flightpath of the drone in terms of coordinates in geojson format to the 'resultfiles/flightpath-date.geojson' file.
     * @param json  The flightpath of the drone in geojson format, as a string.
     * @param date  The date of the orders of this flightpath.
     */
    public static void writeGeojson(String json, String date){
        try{
            FileWriter filewriter = new FileWriter("resultfiles/drone-" + date + ".geojson");
            filewriter.write(json);
            filewriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes all the deliveries and non-deliveries of a given day, along with the outcome to the 'resultfiles/deliveries-date.json' file.
     * @param date          The date of all the orders of this file.
     * @param deliveries    An array of all the deliveries and non-deliveries attempted that day, along with their outcome.
     */
    public static void writeDeliveries(String date, Delivery[] deliveries){
        try{
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(Paths.get("resultfiles/deliveries-" + date + ".json").toFile(), deliveries);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}