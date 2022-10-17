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
}
