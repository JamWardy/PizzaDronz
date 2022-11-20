package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
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
        // check if enough command-line arguments have been entered
        if (args.length < 2) {
            System.err.println("Invalid arguments, please input date and then url");
        } else {
            // parse command-line arguments
            String date = args[0];
            String baseUrlStr = args[1];
            // ensure correct url format
            if (!baseUrlStr.endsWith("/")) {
                baseUrlStr += "/";
            }
            // check if all urls needed exist and are correct
            if (checkURLS(baseUrlStr, date)) {
                try {
                    URL centralURL = new URL(baseUrlStr + "centralArea/");
                    // get all the restaurants and orders from REST server
                    Restaurant[] restaurants = Restaurant.getRestaurantsFromRestServer(new URL(baseUrlStr));
                    Order[] orders = Order.getOrders(baseUrlStr, date);
                    // only make flightpath if there are orders, this prevents errors
                    if (orders.length > 0) {
                        Delivery[] deliveries = Delivery.getDeliveries(orders, restaurants);
                        List<DroneMove> flightpath = new ArrayList<>();
                        // drone starts at Appleton Tower
                        LngLat position = new LngLat(-3.186874, 55.944494);
                        MultiPolygon noFlyZone = LngLat.getNoFlyZone(baseUrlStr);
                        // sort order numbers based off proximity to the restaurant
                        ArrayList<String[]> sortedOrders = Order.sortOrderNos(orders, restaurants, position);
                        // time the start of move calculation
                        long startTicks = System.currentTimeMillis();
                        // for all orders on the day
                        for (String[] orderInfo : sortedOrders) {
                            // get order from its order number
                            Order order = Order.getOrderFromItem(orderInfo[0], orders);
                            // if order is valid
                            if (order.getValidity(restaurants).equals("Valid")) {
                                Restaurant restaurant = order.getRestaurant(restaurants);
                                // construct the path to and from the restaurant
                                List<DroneMove> orderPath = makeOrderPath(position, order, restaurant, noFlyZone, startTicks, centralURL);
                                // if the flightpath would exceed 2000 moves, add the constructed path to the total flightpath and set the order to delivered
                                if (orderPath.size() + flightpath.size() <= 2000) {
                                    flightpath.addAll(orderPath);
                                    Delivery.setDelivered(deliveries, order);
                                    // next drone move starts from end point of the previous flightpath
                                    position = new LngLat(orderPath.get(orderPath.size()-1).toLongitude, orderPath.get(orderPath.size()-1).toLatitude);
                                // otherwise set status to ValidButNotDelivered
                                } else {
                                    Delivery.setValidNotDelivered(deliveries, order);
                                }
                            }
                        }
                        writeFiles(date, deliveries, flightpath);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Creates the 3 required files; deliveries.json, drone.geojson, flightpath.json
     * @param date  Date all the pizzas have been ordered on.
     * @param deliveries    The delivery information for each of the pizzas.
     * @param flightpath    The flightpath the drone took.
     */
    public static void writeFiles(String date, Delivery[] deliveries, List<DroneMove> flightpath){
        List<Point> coordinates = makePathCoordinates(flightpath);
        writeGeojson(FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordinates))).toJson(), date);
        writeDeliveries(date, deliveries);
        writeFlightPath(flightpath, date);
    }

    /**
     * Check that all the URLs that are required during the running of the program exist and are valid.
     * @param baseUrlStr    The base of the URL through which the REST server is accessed.
     * @param date  The date the pizzas have been ordered on.
     * @return  A boolean for whether the URLs required are valid.
     */
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
     * @param flightpath    The flight path of the drone.
     * @param noFlyZone     The No-Fly Zone the drone avoids.
     * @param baseUrlStr    The base URL from which the central area's co-ordinates are requested.
     * @param date          The date the flight path is generated for.
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
     * @param position  Starting point of the drone.
     * @param goal      Finishing point of the drone's flightpath.
     * @param noFlyZone The No-Fly zones of the central area, which the drone should avoid flying through.
     * @param order     The order which the drone is attempting to deliver.
     * @param startTicks         The initial number of ticks of the first calculation.
     * @param stayInCentral Whether the drone should attempt to stay in the central area once inside.
     * @param centralURL    URL of the Central Area
     * @return          A list of drone moves generated for this part of the flightpath.
     */
    public static List<DroneMove> orderPathToGoal(LngLat position, LngLat goal, MultiPolygon noFlyZone, Order order, long startTicks, boolean stayInCentral, URL centralURL){
        List<DroneMove> orderPath = new ArrayList<>();
        List<LngLat> explored = new ArrayList<>();
        while (!position.closeTo(goal)) {
            float bestMove = LngLat.findBestMove(position, goal, noFlyZone, explored, stayInCentral, centralURL);
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

    /**
     * Takes the json formatted flightpath and turns it into a List of points the drone moves through.
     * @param flightpath    The full flightpath of the drone.
     * @return  The list of points that the drone moves through.
     */
    public static List<Point> makePathCoordinates(List<DroneMove> flightpath){
        List<Point> coordinates = new ArrayList<>();
        for (DroneMove move : flightpath) {
            coordinates.add(Point.fromLngLat(move.fromLongitude, move.fromLatitude));
        }
        coordinates.add(Point.fromLngLat(flightpath.get(flightpath.size() - 1).fromLongitude, flightpath.get(flightpath.size() - 1).fromLatitude));
        return coordinates;
    }

    /**
     * Makes the flightpath of the drone for one specific order, starting and finishing close to a specific point and going to a specific restaurant.
     * @param position  Starting position of the drone.
     * @param order The order the drone is delivering.
     * @param restaurant    The restaurant the drone is delivering from.
     * @param noFlyZone The No-Fly zone the drone avoids.
     * @param startTicks    The number of ticks the flightpath calculation started at.
     * @return
     */
    public static List<DroneMove> makeOrderPath(LngLat position, Order order, Restaurant restaurant, MultiPolygon noFlyZone, long startTicks, URL baseURL){
        List<DroneMove> orderPath = (orderPathToGoal(position, new LngLat(restaurant.longitude, restaurant.latitude), noFlyZone, order, startTicks, false, baseURL));
        position = new LngLat(orderPath.get(orderPath.size() - 1).toLongitude, orderPath.get(orderPath.size() - 1).toLatitude);
        orderPath.addAll(orderPathToGoal(position, new LngLat(-3.186874, 55.944494), noFlyZone, order, startTicks, true, baseURL));
        return orderPath;
    }
}