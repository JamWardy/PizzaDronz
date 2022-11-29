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
     * Invokes the program.
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
                    MultiPolygon noFlyZone = NoFlyZone.getNoFlyZones(baseUrlStr);
                    // only make flightpath if there are orders, this prevents errors
                    if (orders.length > 0) {
                        Delivery[] deliveries = Delivery.getDeliveries(orders, restaurants);
                        List<DroneMove> flightpath = new ArrayList<>();
                        // drone starts at Appleton Tower
                        LngLat position = new LngLat(-3.186874, 55.944494);
                        // sort order numbers based off proximity to the restaurant
                        ArrayList<Order> sortedOrders = Order.sortOrders(orders, restaurants, position);
                        // time the start of move calculation
                        long startTicks = System.currentTimeMillis();
                        // for all orders on the day
                        for (Order order : sortedOrders) {
                            // if order is valid
                            if (order.getValidity(restaurants).equals("Valid")) {
                                Restaurant restaurant = order.getRestaurant(restaurants); // restaurants wont be null as it would have caused a jackson exception
                                // construct the path to and from the restaurant
                                List<DroneMove> orderPath = makeFullOrderPath(position, order, restaurant, noFlyZone, startTicks, centralURL);
                                // if the flightpath would exceed 2000 moves, add the constructed path to the total flightpath and set the order to delivered
                                if (orderPath.size() + flightpath.size() <= 2000) {
                                    flightpath.addAll(orderPath);
                                    Delivery.setDelivered(deliveries, order);
                                    // next drone move starts from end point of the previous flightpath
                                    position = new LngLat(orderPath.get(orderPath.size()-1).getToLongitude(), orderPath.get(orderPath.size()-1).getToLatitude());
                                // otherwise set status to ValidButNotDelivered
                                } else {
                                    Delivery.setValidNotDelivered(deliveries, order);
                                }
                            }
                        }
                        writeFiles(date, deliveries, flightpath);
                    }
                } catch (IOException e) {
                    System.err.println("I/O Exception, please check connection and input data");
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
        List<Point> coordinates = DroneMove.makePathCoordinates(flightpath);
        writeGeojson(FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(coordinates))).toJson(), date);
        writeDeliveries(date, deliveries);
        writeFlightPath(flightpath, date);
    }

    /**
     * Check that all the URLs that are required during the running of the program exist and are valid, ie do not trigger an exception.
     * @param baseUrlStr    The base of the URL through which the REST server is accessed.
     * @param date  The date the pizzas have been ordered on.
     * @return  A boolean for whether the URLs required are valid.
     */
    public static boolean checkURLS(String baseUrlStr, String date){
        boolean ok = false;
        try{
            // check creating the URLs does not create malformed URL exception
            URL baseUrl = new URL(baseUrlStr);
            URL orderUrl = new URL(baseUrl + "orders/" + date);
            URL centralAreaURL = new URL(baseUrl + "centralArea/");
            URL noFlyUrl = new URL(baseUrl + "noFlyZones/");
            URL restaurantsUrl = new URL(baseUrl + "restaurants/");
            // check retrieving and parsing the data does not give an error
            new ObjectMapper().readValue(orderUrl, Order[].class);
            new ObjectMapper().readValue(centralAreaURL, LngLat[].class);
            new ObjectMapper().readValue(noFlyUrl, NoFlyZone[].class);
            new ObjectMapper().readValue(restaurantsUrl, Restaurant[].class);
            // if no error, the URLs are fine
            ok = true;
        }
        catch (MalformedURLException e){
            System.err.println("Malformed URL, please check URL protocol");
        }
        catch (IOException e){
            System.err.println("I/O Exception, please check connection and input data");
        }
        return ok;
    }

    /**
     * Generates the list of drone moves for part of the drone's flightpath from the starting position to the goal.
     * @param position  Starting point of the drone.
     * @param goal      Finishing point of the drone's flightpath.
     * @param noFlyZone The No-Fly zones of the central area, which the drone should avoid flying through.
     * @param order     The order which the drone is attempting to deliver.
     * @param startTicks         The initial number of ticks of the first calculation.
     * @param centralURL    URL of the Central Area
     * @return          A list of drone moves generated for this part of the flightpath.
     */
    public static List<DroneMove> makeOrderPathToGoal(LngLat position, LngLat goal, MultiPolygon noFlyZone, Order order, long startTicks, URL centralURL){
        List<DroneMove> orderPath = new ArrayList<>();
        List<LngLat> explored = new ArrayList<>();
        // find moves until goal is reached
        while (!position.closeTo(goal)) {
            float bestMove = findBestMove(position, goal, noFlyZone, explored, centralURL);
            LngLat newPosition = position.nextPosition(bestMove);
            // add the move to the flight path for that order
            orderPath.add(new DroneMove(order.getOrderNo(), position.longitude(), position.latitude(), Float.toString(bestMove), newPosition.longitude(), newPosition.latitude(), System.currentTimeMillis() - startTicks));
            // mark the position of the drone as explored
            explored.add(position);
            position = newPosition;
        }
        orderPath.add(new DroneMove(order.getOrderNo(), position.longitude(), position.latitude() , position.longitude(), position.latitude(), System.currentTimeMillis() - startTicks));
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
     * Writes the flightpath of the drone in terms of coordinates in geojson format to the 'resultfiles/drone-date.geojson' file.
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
     * Makes the flightpath of the drone for one specific order, starting and finishing close to a specific point and going to a specific restaurant.
     * @param position  Starting position of the drone.
     * @param order The order the drone is delivering.
     * @param restaurant    The restaurant the drone is delivering from.
     * @param noFlyZone The No-Fly zone the drone avoids.
     * @param startTicks    The number of ticks the flightpath calculation started at.
     * @param centralURL    URL from which the central area is accessed.
     * @return  Section of the flightpath, as a list of drone moves.
     */
    public static List<DroneMove> makeFullOrderPath(LngLat position, Order order, Restaurant restaurant, MultiPolygon noFlyZone, long startTicks, URL centralURL){
        // make the one-way order path from the start location to the restaurant for the order
        List<DroneMove> orderPath = (makeOrderPathToGoal(position, new LngLat(restaurant.getLongitude(), restaurant.getLatitude()), noFlyZone, order, startTicks, centralURL));
        // iterate through the flight path in reverse order
        for (int i = orderPath.size() - 2; i >= 0; i--){
            // move the drone in the reverse direction as the move in the path
            float newAngle = ((Float.parseFloat(orderPath.get(i).getAngle())+ 180) % 360);
            LngLat pos = new LngLat(orderPath.get(orderPath.size()-1).getToLongitude(), orderPath.get(orderPath.size()-1).getToLatitude());
            orderPath.add(new DroneMove(order.getOrderNo(), orderPath.get(orderPath.size()-1).getToLongitude(), orderPath.get(orderPath.size()-1).getToLatitude(), Float.toString(newAngle), pos.nextPosition(newAngle).longitude(), pos.nextPosition(newAngle).latitude(), System.currentTimeMillis() - startTicks));
        }
        orderPath.add(new DroneMove(order.getOrderNo(), orderPath.get(orderPath.size()-1).getToLongitude(), orderPath.get(orderPath.size()-1).getToLatitude(),  orderPath.get(orderPath.size()-1).getToLongitude(), orderPath.get(orderPath.size()-1).getToLatitude(), System.currentTimeMillis() - startTicks));
        return orderPath;
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
                boolean intersects = NoFlyZone.intersectsNoFlyZones(noFlyZone, position, i);
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