package uk.ac.ed.inf;

import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.execution.LoaderDelegate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.time.*;

/**
 * Class which contains the getDeliveryCost() method
 */
public class Order {
    public String orderNo;
    public String orderDate;
    public String customer;
    public String creditCardNumber;
    public String creditCardExpiry;
    public String cvv;
    public int priceTotalInPence;
    public String[] orderItems;

    public Order(){};

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
        boolean sameRestaurant = true;
        for (Restaurant restaurant: restaurants) {
            sameRestaurant = true; //boolean that identifies whether all items ordered are from this restaurant
            //check each item that has been ordered
            for (String orderItem : orderItems) {
                boolean orderItemInRestaurant = false; //boolean for whether this item ordered is on the menu for this restaurant
                //see if that item is in the restaurant's menu
                for (Menu item : restaurant.menu) {
                    if (item.name.equals(orderItem)) { //if the names of the item ordered and the menu item are the same, this item is on the menu for this restaurant
                        orderItemInRestaurant = true;
                        cost += item.priceInPence; //add item cost to total delivery cost
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

    public static Order[] getOrders(String baseUrl, String date){
        try{
            URL url = new URL(baseUrl + "orders/" + date);
            Order[] orders = new ObjectMapper().readValue(url, Order[].class);
            return orders;
        }
        catch (IOException e){
            e.printStackTrace();
            return new Order[0];
        }
    }

    public static Order[] getOrdersNoDate(String baseUrl){
        try{
            URL url = new URL(baseUrl + "orders/");
            Order[] orders = new ObjectMapper().readValue(url, Order[].class);
            return orders;
        }
        catch (IOException e){
            e.printStackTrace();
            return new Order[0];
        }
    }

    public boolean isCVVValid(){
        return cvv.matches("\\d{3}");
    }

    public boolean isCardNumberValid(){
        return creditCardNumber.matches("\\d{16}");
    }

    public boolean isCardExpiryValid(String date){
        try {
            LocalDate expiryDate = YearMonth.parse(creditCardExpiry, DateTimeFormatter.ofPattern("MM/yy")).atEndOfMonth();
            LocalDate orderDate = LocalDate.parse(date);
            return expiryDate.isAfter(orderDate);
        } catch(Exception e){
            return false;
        }
    }

    public boolean isPizzaValid(String pizza, Restaurant[] restaurants){
        for (Restaurant restaurant: restaurants){
            for (Menu menu: restaurant.getMenu()){
                if (menu.name.equals(pizza)){
                    return true;
                }
            }
        }
        return false;
    }

    public boolean pizzasDefined(Restaurant[] restaurants){
        for (String pizza: orderItems){
            if (!isPizzaValid(pizza, restaurants)){
                return false;
            }
        }
        return true;
    }

    public boolean validPizzaCount(){
        return (orderItems.length >= 1 && orderItems.length <= 4);
    }

    public boolean sameSuppliers(Restaurant[] restaurants){
        try {
            Order.getDeliveryCost(restaurants, orderItems);
            return true;
        } catch(InvalidPizzaCombinationException e){
            return false;
        }
    }

    public boolean correctTotal(Restaurant[] restaurants){
        try{
            return (Order.getDeliveryCost(restaurants, orderItems) == priceTotalInPence);
        }
        catch (InvalidPizzaCombinationException e){
            return false;
        }
    }

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
}
