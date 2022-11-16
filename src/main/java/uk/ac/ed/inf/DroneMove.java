package uk.ac.ed.inf;

import javax.annotation.Nullable;

/**
 * Represents a drone move, in the format specified in the flightpath json file.
 */
@Nullable
public class DroneMove {
    public String orderNo;
    public double fromLongitude;
    public double fromLatitude;
    public String angle = "null";
    public double toLongitude;
    public double toLatitude;
    public long ticksSinceStartOfCalculation;

    /**
     * Constructor with a non-null move angle.
     * @param orderNo   Number of the order being delivered, as a String.
     * @param fromLongitude Longitude of the drone before the move.
     * @param fromLatitude  Latitude of the drone before the move.
     * @param angle Angle of the drone move.
     * @param toLongitude   Longitude of the drone after the move.
     * @param toLatitude    Latitude of the drone after the move.
     * @param ticksSinceStartOfCalculation  Number of milliseconds elapsed since the first drone move was calculated.
     */
    public DroneMove(String orderNo, double fromLongitude, double fromLatitude, String angle, double toLongitude, double toLatitude, long ticksSinceStartOfCalculation) {
        this.orderNo = orderNo;
        this.fromLongitude = fromLongitude;
        this.fromLatitude = fromLatitude;
        this.angle = angle;
        this.toLongitude = toLongitude;
        this.toLatitude = toLatitude;
        this.ticksSinceStartOfCalculation = ticksSinceStartOfCalculation;
    }

    /**
     * Constructor with a null move angle, for hovering.
     * @param orderNo   Number of the order being delivered, as a String.
     * @param fromLongitude Longitude of the drone before the move.
     * @param fromLatitude  Latitude of the drone before the move.
     * @param toLongitude   Longitude of the drone after the move.
     * @param toLatitude    Latitude of the drone after the move.
     * @param ticksSinceStartOfCalculation  Number of milliseconds elapsed since the first drone move was calculated.
     */
    public DroneMove(String orderNo, double fromLongitude, double fromLatitude, double toLongitude, double toLatitude, long ticksSinceStartOfCalculation) {
        this.orderNo = orderNo;
        this.fromLongitude = fromLongitude;
        this.fromLatitude = fromLatitude;
        this.toLongitude = toLongitude;
        this.toLatitude = toLatitude;
        this.ticksSinceStartOfCalculation = ticksSinceStartOfCalculation;
    }
}
