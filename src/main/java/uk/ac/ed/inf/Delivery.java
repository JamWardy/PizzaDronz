package uk.ac.ed.inf;

/**
 * Delivery outcome of a specific order, as specified for the deliveries.json file
 */
public class Delivery {
    private final String orderNo;
    private String outcome;
    private final int costInPence;

    /**
     * Default constructor for the Delivery class
     * @param orderNo       The order number of the order being delivered, as a String.
     * @param outcome       The outcome of the order, this is a String which can take the values specified in the OrderOutcome Enum or 'Valid' (before flightpath calculated)
     * @param costInPence   The cost of the order in pence, as an integer.
     */
    public Delivery(String orderNo, String outcome, int costInPence){
        this.orderNo = orderNo;
        this.outcome = outcome;
        this.costInPence = costInPence;
    }

    public String getOrderNo() {
        return orderNo;
    }

    /**
     *
     * @return  Order outcome as a string, can take the values defined in the OrderOutcome Enum.
     */
    public String getOutcome() {
        return outcome;
    }

    public int getCostInPence() {
        return costInPence;
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
            // sets the order's outcome to the reason if invalid, else valid
            deliveries[i] = new Delivery(orders[i].getOrderNo(), orders[i].getValidity(restaurants), orders[i].getPriceTotalInPence());
        }
        return deliveries;
    }

    /**
     * Changes the outcome of a specific order in an array of deliveries to Delivered.
     * @param deliveries    Array of Delivery objects.
     * @param order         The Order object for which the outcome needs to be changed.
     */
    public static void setDelivered(Delivery[] deliveries, Order order){
        for (Delivery delivery: deliveries){
            if (delivery.orderNo.equals(order.getOrderNo())){
                delivery.outcome = OrderOutcome.Delivered.toString();
            }
        }
    }

    /**
     * Changes the outcome of a specific order in an array of deliveries to ValidButNotDelivered.
     * @param deliveries    Array of Delivery objects.
     * @param order         The Order object for which the outcome needs to be changed.
     */
    public static void setValidNotDelivered(Delivery[] deliveries, Order order){
        for (Delivery delivery: deliveries){
            if (delivery.orderNo.equals(order.getOrderNo())){
                delivery.outcome = OrderOutcome.ValidButNotDelivered.toString();
            }
        }
    }
}
