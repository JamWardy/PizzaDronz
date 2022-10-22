package uk.ac.ed.inf;

public class DroneMove {
    public String orderNo;
    public double fromLongitude;
    public double fromLatitude;
    public float angle;
    public double toLongitude;
    public double toLatitude;
    public int ticksSinceStartOfCalculation;

    public DroneMove(String orderNo, double fromLongitude, double fromLatitude, float angle, double toLongitude, double toLatitude, int ticksSinceStartOfCalculation) {
        this.orderNo = orderNo;
        this.fromLongitude = fromLongitude;
        this.fromLatitude = fromLatitude;
        this.angle = angle;
        this.toLongitude = toLongitude;
        this.toLatitude = toLatitude;
        this.ticksSinceStartOfCalculation = ticksSinceStartOfCalculation;
    }
}
