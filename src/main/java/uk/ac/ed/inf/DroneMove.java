package uk.ac.ed.inf;

import javax.annotation.Nullable;

/**
 * Represents a drone move, in the format specified in the flightpath .json file.
 */
@Nullable
public class DroneMove {
    private final String orderNo;
    private final double fromLongitude;
    private final double fromLatitude;
    private String angle = "null";
    private final double toLongitude;
    private final double toLatitude;
    private final long ticksSinceStartOfCalculation;

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

    /**
     *
     * @return Order number of the order being delivered.
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     *
     * @return Longitude of the drone before the move.
     */
    public double getFromLongitude(){
        return fromLongitude;
    }

    /**
     *
     * @return Latitude of the drone before the move.
     */
    public double getFromLatitude(){
        return fromLatitude;
    }

    /**
     *
     * @return Angle the drone moved in, as a string. Can be between 0 and 337.5 degrees, in increments of 22.5 degrees, or null when hovering.
     */
    public String getAngle(){
        return angle;
    }

    /**
     *
     * @return Longitude of the drone after the move.
     */
    public double getToLongitude() {
        return toLongitude;
    }

    /**
     *
     * @return Latitude of the drone after the move.
     */
    public double getToLatitude(){
        return  toLatitude;
    }

    /**
     *
     * @return  Number of milliseconds elapsed since the drone's flightpath started being calculated.
     */
    public long getTicksSinceStartOfCalculation() {
        return ticksSinceStartOfCalculation;
    }
}
