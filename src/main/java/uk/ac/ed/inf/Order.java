package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.time.*;
import java.util.Comparator;

/**
 * Class that represents an order made
 */
public record Order(String orderNo, String orderDate, String customer, String creditCardNumber, String creditCardExpiry, String cvv, int priceTotalInPence, String[] orderItems) {

    public String getOrderNo(){
        return orderNo;
    }

    public int getPriceTotalInPence(){
        return priceTotalInPence;
    }

    /**
     * Returns the cost in pence of having a set of items (passed as a parameter)
     * ordered from a set of restaurants (also passed as a parameter) if it is possible to deliver all those items.
     * Throws an InvalidPizzaCombinationException if not all items ordered belong to the same restaurant.
     * @param restaurants An array containing all the restaurants it is possible to order from.
     * @param orderItems These are the names of the items that have been ordered, all passed in individually. There should be between 1 and 4 of these items and they should all be from the same restaurant.
     * @return The cost in pence of having all the items delivered, if it is a valid delivery.
     */
    public static int getDeliveryCost(Restaurant[] restaurants, String... orderItems){
        if (orderItems.length == 0){
            throw new InvalidPizzaCombinationException("No Pizzas Ordered");
        }
        else if(orderItems.length > 4){
            throw new InvalidPizzaCombinationException("Too Many Pizzas Ordered");
        }
        int cost = 0;
        //check if all order items are from the same restaurant
        boolean sameRestaurant;
        for (Restaurant restaurant: restaurants) {
            sameRestaurant = true; //boolean that identifies whether all items ordered are from this restaurant
            //check each item that has been ordered
            for (String orderItem : orderItems) {
                boolean orderItemInRestaurant = false; //boolean for whether this item ordered is on the menu for this restaurant
                //see if that item is in the restaurant's menu
                for (Item item : restaurant.getMenu()) {
                    if (item.getName().equals(orderItem)) { //if the names of the item ordered and the menu item are the same, this item is on the menu for this restaurant
                        orderItemInRestaurant = true;
                        cost += item.getPriceInPence(); //add item cost to total delivery cost
                        break;
                    }
                }
                //if this item is not in the menu for this restaurant, then this is not the restaurant all orders come from
                if (!orderItemInRestaurant) {
                    sameRestaurant = false;
                }
            }
            //if all items have been ordered from this restaurant, then return the delivery cost of all the items
            if (sameRestaurant){
                return cost + 100;
            }
        }
        throw new InvalidPizzaCombinationException("Invalid Pizza Combination");
    }

    /**
     * Gets the json of orders from the REST server at 'baseUrl/orders/date' and returns this as an array of Order objects.
     * @param baseUrl   The base URL to which the request is made.
     * @param date      The date of the orders which are being requested.
     * @return          An array of Order objects, which is all the orders made on the corresponding day.
     */
    public static Order[] getOrders(String baseUrl, String date){
        try{
            URL url = new URL(baseUrl + "orders/" + date);
            return new ObjectMapper().readValue(url, Order[].class);
        }
        catch (IOException e){
            e.printStackTrace();
            return new Order[0];
        }
    }

    /**
     * Checks whether the CVV is in the valid format of 3 digits.
     * @return Boolean for whether the CVV is valid.
     */
    public boolean isCVVValid(){
        return cvv.matches("\\d{3}");
    }

    /**
     * Checks whether the card number is in the valid format of 16 digits.
     * @return  Boolean for whether the card number is valid.
     */
    public boolean isCardNumberValid(){
        return creditCardNumber.matches("\\d{16}");
    }

    /**
     * Checks whether the card expiry date is after the date of the order.
     * @param date  The card's expiry date in MM/yy format.
     * @return  Boolean for whether the card expiry date is valid.
     */
    public boolean isCardExpiryValid(String date){
        try {
            LocalDate expiryDate = YearMonth.parse(creditCardExpiry, DateTimeFormatter.ofPattern("MM/yy")).atEndOfMonth();
            LocalDate orderDate = LocalDate.parse(date);
            return expiryDate.isAfter(orderDate);
        } catch(Exception e){
            return false;
        }
    }

    /**
     * Checks whether a specific pizza exists on any of the restaurant's menus.
     * @param pizza         The name of pizza being checked.
     * @param restaurants   Array of restaurants items can be ordered from.
     * @return  Boolean for whether the pizza exists.
     */
    public boolean isPizzaValid(String pizza, Restaurant[] restaurants){
        for (Restaurant restaurant: restaurants){
            for (Item item: restaurant.getMenu()){
                if (item.getName().equals(pizza)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether all the pizzas ordered in the order exist.
     * @param restaurants   List of restaurants pizzas can be ordered from.
     * @return  Boolean for whether all the pizzas exist.
     */
    public boolean pizzasDefined(Restaurant[] restaurants){
        for (String pizza: orderItems){
            if (!isPizzaValid(pizza, restaurants)){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the number of pizzas ordered is valid, that is between 1 and 4.
     * @return  Boolean for if the number of pizzas ordered is valid.
     */
    public boolean validPizzaCount(){
        return (orderItems.length >= 1 && orderItems.length <= 4);
    }

    /**
     * Checks if all the pizzas ordered come from the same supplier.
     * @param restaurants   Array of restaurants that pizzas can be ordered from.
     * @return  Boolean for whether the pizzas all come from the same supplier.
     */
    public boolean sameSuppliers(Restaurant[] restaurants){
        try {
            Order.getDeliveryCost(restaurants, orderItems);
            return true;
        } catch(InvalidPizzaCombinationException e){
            return false;
        }
    }

    /**
     * Checks if the order total cost is the same as the expected order total cost for those items.
     * @param restaurants   Array of restaurants pizzas can be ordered from, as Restaurant objects.
     * @return  Whether the order total cost matches the expected total order cost.
     */
    public boolean correctTotal(Restaurant[] restaurants){
        try{
            return (Order.getDeliveryCost(restaurants, orderItems) == priceTotalInPence);
        }
        catch (InvalidPizzaCombinationException e){
            return false;
        }
    }

    /**
     * Returns the order outcome of an order.
     * @param restaurants   An array of restaurants that pizzas can be ordered from, each as a Restaurant object.
     * @return  The corresponding OrderOutcome Enum as a string if the order is not valid, or the String "valid" if it is.
     */
    public String getValidity(Restaurant[] restaurants){
        if (!isCardNumberValid()){
            return OrderOutcome.InvalidCardNumber.toString();
        }
        else if (!isCardExpiryValid(orderDate)){
            return OrderOutcome.InvalidExpiryDate.toString();
        }
        else if (!isCVVValid()){
            return OrderOutcome.InvalidCvv.toString();
        }
        else if (!validPizzaCount()){
            return OrderOutcome.InvalidPizzaCount.toString();
        }
        else if (!pizzasDefined(restaurants)){
            return OrderOutcome.InvalidPizzaNotDefined.toString();
        }
        else if (!sameSuppliers(restaurants)){
            return OrderOutcome.InvalidPizzaCombinationMultipleSuppliers.toString();
        }
        else if(!correctTotal(restaurants)){
            return OrderOutcome.InvalidTotal.toString();
        }
        else {
            return "Valid";
        }
    }

    /**
     * Gets the restaurant from which this order's pizzas have been ordered from (assuming the order is valid)
     * @param restaurants   An array of restaurants pizzas can be ordered from, each as Restaurant objects.
     * @return  The restaurant from which the pizzas have been ordered.
     */
    public Restaurant getRestaurant(Restaurant[] restaurants){
        for (Restaurant restaurant: restaurants){
            for (Item item: restaurant.getMenu()){
                if (item.getName().equals(this.orderItems[0])){
                    return restaurant;
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of orders, sorted in ascending order by distance between a starting position and the restaurant the order is picked up from .
     * @param orders    List of orders the drone is trying to deliver.
     * @param restaurants   List of restaurants pizzas can be ordered from.
     * @param position  Starting position of the drone.
     * @return  A sorted list of pairs of orders.
     */
    public static ArrayList<Order> sortOrders(Order[] orders, Restaurant[] restaurants, LngLat position){
        ArrayList<String[]> orderInfos = new ArrayList<>();
        for (Order order: orders){
            // distance from the given position to the order's restaurant, note the coordinates wont be null as it would have caused a jackson error
            double dist = position.distanceTo(new LngLat(order.getRestaurant(restaurants).getLongitude(),order.getRestaurant(restaurants).getLatitude()));
            // create order number, distance pair
            String[] orderInfo = {order.orderNo, Double.toString(dist)};
            orderInfos.add(orderInfo);
        }
        // sort the pairs by distance
        orderInfos.sort(Comparator.comparingDouble(o -> Double.parseDouble(o[1])));
        // turn the pairs into a list of orders in the sorted arrangement
        ArrayList<Order> sortedOrders = new ArrayList<>();
        for (String[] each: orderInfos){
            sortedOrders.add(getOrderFromOrderNo(each[0], orders));
        }
        return sortedOrders;
    }

    /**
     * Gets the order object from its order number. If order can't be found returns the first order in the list.
     * @param orderNumber   The order number of the required order.
     * @param orders    All the orders.
     * @return  The order object corresponding to the order number.
     */
    public static Order getOrderFromOrderNo(String orderNumber, Order[] orders){
        Order order = orders[0];
        for (Order temporder : orders) {
            if (temporder.orderNo.equals(orderNumber)) {
                order = temporder;
            }
        }
        return order;
    }
}
