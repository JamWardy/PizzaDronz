package uk.ac.ed.inf;

public class Order {
    public int getDeliveryCost(Restaurant[] restaurants, String... orderItems){
        int cost = 0;
        boolean sameRestaurant = true;
        for (Restaurant restaurant: restaurants) {
            sameRestaurant = true;
            for (String orderItem : orderItems) {
                boolean orderItemInRestaurant = false;
                for (Menu item : restaurant.menu) {
                    if (item.name.equals(orderItem)) {
                        orderItemInRestaurant = true;
                        cost += item.priceInPence;
                        break;
                    }
                }
                if (!orderItemInRestaurant) {
                    sameRestaurant = false;
                }
            }
            if (sameRestaurant){
                return cost + 100;
            }
        }
        throw new InvalidPizzaCombinationException("Invalid Pizza Combination");
    }
}
