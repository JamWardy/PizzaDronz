package uk.ac.ed.inf;

/**
 * Class which represents a delivery outcome, as specified for the deliveries.json file
 */
public class Delivery {
    public String orderNo;
    public String outcome;
    public int costInPence;

    /**
     * Default constructor for the Delivery class
     * @param orderNo       The order number of the order being delivered, as a String.
     * @param outcome       The outcome of the order, this is a String which can take the values specified in the OrderOutcome Enum.
     * @param costInPence   The cost of the order in pence, as an integer.
     */
    public Delivery(String orderNo, String outcome, int costInPence){
        this.orderNo = orderNo;
        this.outcome = outcome;
        this.costInPence = costInPence;
    }

    /**
     * Takes in all the orders and returns an array of Delivery objects, with the validity of each order.
     * @param orders        Array of all the orders to be turned into Delivery objects.
     * @param restaurants   Array of all the restaurants pizzas can be ordered from.
     * @return An array of Delivery objects.
     */
    public static Delivery[] getDeliveries(Order[] orders, Restaurant[] restaurants){
        Delivery[] deliveries = new Delivery[orders.length];
        for (int i = 0; i < orders.length; i++) {
            deliveries[i] = new Delivery(orders[i].orderNo, orders[i].getValidity(restaurants), orders[i].priceTotalInPence);
        }
        return deliveries;
    }

    /**
     * Changes the outcome of a specific order in an array of deliveries to Delivered.
     * @param deliveries    Array of Delivery objects.
     * @param order         The Order object for which the outcome needs to be changed.
     * @return  An array of Delivery objects, with the outcome changed.
     */
    public static void setDelivered(Delivery[] deliveries, Order order){
        for (Delivery delivery: deliveries){
            if (delivery.orderNo.equals(order.orderNo)){
                delivery.outcome = OrderOutcome.Delivered.toString();
            }
        }
    }

    /**
     * Changes the outcome of a specific order in an array of deliveries to ValidButNotDelivered.
     * @param deliveries    Array of Delivery objects.
     * @param order         The Order object for which the outcome needs to be changed.
     * @return  An array of Delivery objects, with the outcome changed.
     */
    public static void setValidNotDelivered(Delivery[] deliveries, Order order){
        for (Delivery delivery: deliveries){
            if (delivery.orderNo.equals(order.orderNo)){
                delivery.outcome = OrderOutcome.ValidButNotDelivered.toString();
            }
        }
    }
}
