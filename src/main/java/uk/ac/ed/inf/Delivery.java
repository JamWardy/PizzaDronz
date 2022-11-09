package uk.ac.ed.inf;

public class Delivery {
    public String orderNo;
    public String outcome;
    public int costInPence;
    public Delivery(String orderNo, String outcome, int costInPence){
        this.orderNo = orderNo;
        this.outcome = outcome;
        this.costInPence = costInPence;
    }

    public static Delivery[] getDeliveries(Order[] orders, Restaurant[] restaurants){
        Delivery[] deliveries = new Delivery[orders.length];
        for (int i = 0; i < orders.length; i++) {
            deliveries[i] = new Delivery(orders[i].orderNo, orders[i].getValidity(restaurants), orders[i].priceTotalInPence);
        }
        return deliveries;
    }
}
