package uk.ac.ed.inf;

/**
 * Class which contains the getDeliveryCost() method
 */
public class Order {
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
}
