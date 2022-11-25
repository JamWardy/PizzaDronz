package uk.ac.ed.inf;

import com.mapbox.geojson.Point;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a drone move, in the format specified in the flightpath .json file.
 */
@Nullable
public class DroneMove {
    private final String orderNo;
    private final double fromLongitude;
    private final double fromLatitude;
    private final String angle;
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
        this.angle = "null";
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

    /**
     * Takes the json formatted flightpath and turns it into a List of points the drone moves through.
     * @param flightpath    The full flightpath of the drone.
     * @return  The list of points that the drone moves through.
     */
    public static List<Point> makePathCoordinates(List<DroneMove> flightpath){
        List<Point> coordinates = new ArrayList<>();
        for (DroneMove move : flightpath) {
            coordinates.add(Point.fromLngLat(move.getFromLongitude(), move.getFromLatitude()));
        }
        coordinates.add(Point.fromLngLat(flightpath.get(flightpath.size() - 1).getFromLongitude(), flightpath.get(flightpath.size() - 1).getFromLatitude()));
        return coordinates;
    }
}
